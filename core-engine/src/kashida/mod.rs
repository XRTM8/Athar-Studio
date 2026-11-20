//! High-Performance Smart Kashida & Calligraphic Elongation Engine for Athar (أَثَــر)
//!
//! Fully supports Arabic diacritics (Harakat / Tashkeel), non-joining boundaries,
//! and accurate calligraphic placement rules.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ArabicJoiningType {
    DualJoining,
    RightJoining,
    LeftJoining,
    NonJoining,
    Transparent,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct KashidaCandidate {
    pub char_index: usize,
    pub insert_after_index: usize,
    pub unicode_char: char,
    pub priority: u8,
    pub recommended_length_ratio: f32,
}

pub struct KashidaEngine;

impl KashidaEngine {
    #[inline(always)]
    pub fn is_diacritic(c: char) -> bool {
        matches!(
            c,
            '\u{064B}'..='\u{065F}' // Fathatan, Dammatan, Kasratan, Fatha, Damma, Kasra, Shadda, Sukun
            | '\u{0670}'             // Dagger Alef
            | '\u{06D6}'..='\u{06DC}' // Quranic marks
            | '\u{06DF}'..='\u{06E8}'
            | '\u{06EA}'..='\u{06ED}'
        )
    }

    #[inline(always)]
    pub fn get_joining_type(c: char) -> ArabicJoiningType {
        if Self::is_diacritic(c) {
            return ArabicJoiningType::Transparent;
        }

        match c {
            // Right-joining only (cannot extend forward to the next letter)
            'ا' | 'أ' | 'إ' | 'آ' | 'ٱ' |
            'د' | 'ذ' |
            'ر' | 'ز' |
            'و' | 'ؤ' |
            'ة' => ArabicJoiningType::RightJoining,

            // Non-joining
            'ء' => ArabicJoiningType::NonJoining,

            // Dual-joining (connecting on both sides)
            'ب' | 'ت' | 'ث' |
            'ج' | 'ح' | 'خ' |
            'س' | 'ش' |
            'ص' | 'ض' |
            'ط' | 'ظ' |
            'ع' | 'غ' |
            'ف' | 'ق' |
            'ك' | 'ل' | 'م' | 'ن' |
            'ه' | 'ي' | 'ى' | 'ئ' => ArabicJoiningType::DualJoining,

            // Whitespace / punctuation / tatweel
            ' ' | '\t' | '\n' | '.' | '،' | '؛' | '؟' | '!' | '-' | 'ـ' => ArabicJoiningType::Transparent,

            _ => ArabicJoiningType::Transparent,
        }
    }

    #[inline(always)]
    pub fn get_calligraphic_priority(c: char, next_c: char, is_pre_final: bool) -> u8 {
        if c == 'ل' && matches!(next_c, 'ا' | 'أ' | 'إ' | 'آ' | 'ٱ') {
            return 0;
        }
        if c == 'م' && matches!(next_c, 'ج' | 'ح' | 'خ') {
            return 1;
        }

        let mut score = match c {
            'س' | 'ش' => 15,
            'ص' | 'ض' => 14,
            'ط' | 'ظ' => 13,
            'ج' | 'ح' | 'خ' => 12,
            'ف' | 'ق' => 11,
            'ع' | 'غ' => 10,
            'ب' | 'ت' | 'ث' | 'ن' | 'ي' | 'ئ' => 9,
            'ك' | 'ل' => 7,
            'م' => 5,
            'ه' => 3,
            _ => 4,
        };

        if is_pre_final {
            score += 10;
        }

        score
    }

    /// Finds valid Kashida candidate positions, properly skipping over all Tashkeel/diacritics
    pub fn find_kashida_opportunities(text: &str) -> Vec<KashidaCandidate> {
        let chars: Vec<(usize, char)> = text.char_indices().collect();
        if chars.len() < 2 {
            return Vec::new();
        }

        let mut candidates = Vec::with_capacity(chars.len() / 2);

        let mut i = 0;
        while i < chars.len() {
            let (curr_byte_idx, current_char) = chars[i];
            let curr_type = Self::get_joining_type(current_char);

            if curr_type == ArabicJoiningType::DualJoining {
                // Find next non-diacritic character, keeping track of where current letter's diacritics end
                let mut insert_after_byte_idx = curr_byte_idx + current_char.len_utf8();
                let mut next_char_idx = i + 1;

                while next_char_idx < chars.len() {
                    let (next_byte_idx, next_char) = chars[next_char_idx];
                    if Self::is_diacritic(next_char) {
                        insert_after_byte_idx = next_byte_idx + next_char.len_utf8();
                        next_char_idx += 1;
                    } else {
                        break;
                    }
                }

                if next_char_idx < chars.len() {
                    let (_, next_char) = chars[next_char_idx];
                    let next_type = Self::get_joining_type(next_char);

                    // A kashida occurs if current connects forward AND next connects backward
                    if next_type == ArabicJoiningType::DualJoining || next_type == ArabicJoiningType::RightJoining {
                        let is_pre_final = next_char_idx + next_char.len_utf8() >= text.len();
                        let priority = Self::get_calligraphic_priority(current_char, next_char, is_pre_final);
                        if priority > 0 {
                            candidates.push(KashidaCandidate {
                                char_index: curr_byte_idx,
                                insert_after_index: insert_after_byte_idx,
                                unicode_char: current_char,
                                priority,
                                recommended_length_ratio: 1.0,
                            });
                        }
                    }
                }
            }

            i += 1;
        }

        candidates.sort_unstable_by_key(|a| std::cmp::Reverse(a.priority));
        candidates
    }

    /// Applies smart elongation, respecting all diacritics
    pub fn apply_smart_kashida(text: &str, stretch_level: u8, target_char_index: Option<usize>) -> String {
        if stretch_level == 0 || text.is_empty() {
            return text.to_string();
        }

        let candidates = Self::find_kashida_opportunities(text);
        if candidates.is_empty() {
            return text.to_string();
        }

        let tatweel_repeat = stretch_level as usize;
        let mut result = String::with_capacity(text.len() + (tatweel_repeat * 4));

        let selected_insertion_point = if let Some(target_idx) = target_char_index {
            candidates.iter().find(|c| c.char_index == target_idx).map(|c| c.insert_after_index)
        } else {
            candidates.first().map(|c| c.insert_after_index)
        };

        let mut current_byte = 0;
        for c in text.chars() {
            result.push(c);
            current_byte += c.len_utf8();

            if Some(current_byte) == selected_insertion_point {
                for _ in 0..tatweel_repeat {
                    result.push('ـ');
                }
            }
        }

        result
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_kashida_with_tashkeel() {
        // "أَثَــر" contains Alef with Fatha, Thaa with Fatha, and Raa
        let text = "أَثَرَ";
        let candidates = KashidaEngine::find_kashida_opportunities(text);
        assert!(!candidates.is_empty(), "Should find kashida after Thaa even with Fatha!");

        let stretched = KashidaEngine::apply_smart_kashida(text, 3, None);
        assert!(stretched.contains('ـ'));
        println!("Stretched text with Tashkeel: {}", stretched);
        assert!(stretched.starts_with("أَثَـ"));
    }

    #[test]
    fn test_plain_text() {
        let text = "أثر";
        let stretched = KashidaEngine::apply_smart_kashida(text, 2, None);
        assert!(stretched.contains('ـ'));
        assert_eq!(stretched, "أثــر");
    }
}
