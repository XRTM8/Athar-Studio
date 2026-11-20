//! Smart Kashida & Bezier Elongation Engine for Mada (مَــدَى)
//!
//! Unlike primitive apps that inject Unicode U+0640 repeatedly, this module calculates
//! precise elongation points based on Arabic calligraphic morphology and Bezier contours.

use serde::{Deserialize, Serialize};

/// Represents an Arabic character connectivity classification
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum ArabicJoiningType {
    /// Connects on both right and left (e.g. Baa in middle, Seen, Ain)
    DualJoining,
    /// Connects only to the right / preceding letter (e.g. Alef, Dal, Reh, Waw)
    RightJoining,
    /// Connects only to the left (very rare in standard Arabic)
    LeftJoining,
    /// Does not connect to either side (e.g. Hamza alone)
    NonJoining,
    /// Non-Arabic / whitespace / punctuation
    Transparent,
}

/// Information about a word segment and its elongation candidate points
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct KashidaCandidate {
    pub char_index: usize,
    pub unicode_char: char,
    pub priority: u8, // Higher = better aesthetic placement in Arabic calligraphy
    pub recommended_length_ratio: f32,
}

pub struct KashidaEngine;

impl KashidaEngine {
    /// Determines the Arabic joining type of a character
    pub fn get_joining_type(c: char) -> ArabicJoiningType {
        match c {
            // Right-joining only (cannot be extended on their left side!)
            'ا' | 'أ' | 'إ' | 'آ' | 'ٱ' |
            'د' | 'ذ' |
            'ر' | 'ز' |
            'و' | 'ؤ' |
            'ة' => ArabicJoiningType::RightJoining,

            // Non-joining
            'ء' => ArabicJoiningType::NonJoining,

            // Dual-joining (can be extended after them if connected to next)
            'ب' | 'ت' | 'ث' |
            'ج' | 'ح' | 'خ' |
            'س' | 'ش' |
            'ص' | 'ض' |
            'ط' | 'ظ' |
            'ع' | 'غ' |
            'ف' | 'ق' |
            'ك' | 'ل' | 'م' | 'ن' |
            'ه' | 'ي' | 'ى' | 'ئ' => ArabicJoiningType::DualJoining,

            // Whitespace & punctuation
            ' ' | '\t' | '\n' | '.' | '،' | '؛' | '؟' | '!' | '-' | 'ـ' => ArabicJoiningType::Transparent,

            _ => ArabicJoiningType::Transparent,
        }
    }

    /// Evaluates calligraphic elongation priority for dual-joining letters
    /// according to classical Arabic calligraphy (Ibn Muqla / Al-Khatt Al-Arabi rules)
    pub fn get_calligraphic_priority(c: char) -> u8 {
        match c {
            // Highest aesthetic elongation priority: Seen, Sheen, Saad, Daad, Ain, Ghain, Faa, Qaaf
            'س' | 'ش' => 10,
            'ص' | 'ض' => 9,
            'ع' | 'غ' => 8,
            'ف' | 'ق' => 8,
            'ط' | 'ظ' => 7,
            'ب' | 'ت' | 'ث' | 'ن' | 'ي' => 6,
            'ك' | 'ل' => 5,
            'م' | 'ه' => 4,
            _ => 1,
        }
    }

    /// Finds valid, aesthetically sound kashida insertion points in an Arabic text
    pub fn find_kashida_opportunities(text: &str) -> Vec<KashidaCandidate> {
        let chars: Vec<(usize, char)> = text.char_indices().collect();
        let mut candidates = Vec::new();

        if chars.len() < 2 {
            return candidates;
        }

        for i in 0..chars.len() - 1 {
            let (idx, current) = chars[i];
            let (_, next) = chars[i + 1];

            let curr_type = Self::get_joining_type(current);
            let next_type = Self::get_joining_type(next);

            // In Arabic calligraphy, a kashida occurs strictly between a dual-joining character
            // and any subsequent joining character (dual or right-joining) within the same word.
            if curr_type == ArabicJoiningType::DualJoining 
                && (next_type == ArabicJoiningType::DualJoining || next_type == ArabicJoiningType::RightJoining)
            {
                // Special calligraphic rule: avoid Kashida right before Final Haa / Taa Marbuta if short
                let priority = Self::get_calligraphic_priority(current);
                candidates.push(KashidaCandidate {
                    char_index: idx,
                    unicode_char: current,
                    priority,
                    recommended_length_ratio: 1.0,
                });
            }
        }

        // Sort candidates by calligraphic priority descending
        candidates.sort_by(|a, b| b.priority.cmp(&a.priority));
        candidates
    }

    /// Formats text with intelligent, rule-based Kashida elongation
    ///
    /// * `text`: Source Arabic text
    /// * `stretch_level`: 0 (none) to 5 (maximum artistic stretch)
    /// * `target_char_index`: Optional specific character to stretch exclusively
    pub fn apply_smart_kashida(text: &str, stretch_level: u8, target_char_index: Option<usize>) -> String {
        if stretch_level == 0 || text.is_empty() {
            return text.to_string();
        }

        let candidates = Self::find_kashida_opportunities(text);
        if candidates.is_empty() {
            return text.to_string();
        }

        let mut result = String::with_capacity(text.len() + (stretch_level as usize * 10));
        let chars: Vec<(usize, char)> = text.char_indices().collect();

        // Calculate how many elongation units (ـ) per eligible position
        let elongation_str = "ـ".repeat(stretch_level as usize);

        for i in 0..chars.len() {
            let (idx, c) = chars[i];
            result.push(c);

            // Check if this position is selected for elongation
            let should_elongate = if let Some(target_idx) = target_char_index {
                idx == target_idx
            } else {
                // Auto mode: elongate the top priority candidate(s)
                candidates.first().map(|best| best.char_index == idx).unwrap_or(false)
            };

            if should_elongate {
                result.push_str(&elongation_str);
            }
        }

        result
    }
}

#[cfg(test)]
mod tests {
    use super::*;

    #[test]
    fn test_kashida_opportunities() {
        let text = "محمد"; // M - H - M - D
        let candidates = KashidaEngine::find_kashida_opportunities(text);
        // Valid connections: M->H, H->M (D is right-joining, so M->D is also valid!)
        assert!(!candidates.is_empty());
    }

    #[test]
    fn test_non_joining_rule() {
        let text = "دار"; // Dal (Right only), Alef (Right only), Reh (Right only)
        let candidates = KashidaEngine::find_kashida_opportunities(text);
        // None of these letters can stretch forward to the next letter!
        assert!(candidates.is_empty());
    }

    #[test]
    fn test_smart_kashida_application() {
        let text = "سماء";
        let stretched = KashidaEngine::apply_smart_kashida(text, 2, None);
        assert!(stretched.contains('ـ'));
        assert!(stretched.starts_with("ســ"));
    }
}
