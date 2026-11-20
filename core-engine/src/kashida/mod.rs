//! High-Performance Smart Kashida & Bezier Elongation Engine for Athar (أَثَــر)
//!
//! Architected for zero-unnecessary-allocations and instant (sub-millisecond)
//! Arabic calligraphic morphology analysis.

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
    pub unicode_char: char,
    pub priority: u8,
    pub recommended_length_ratio: f32,
}

pub struct KashidaEngine;

impl KashidaEngine {
    #[inline(always)]
    pub fn get_joining_type(c: char) -> ArabicJoiningType {
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

            // Transparent / diacritics / whitespace / tatweel
            ' ' | '\t' | '\n' | '.' | '،' | '؛' | '؟' | '!' | '-' | 'ـ' |
            '\u{064B}'..='\u{065F}' | '\u{0670}' => ArabicJoiningType::Transparent,

            _ => ArabicJoiningType::Transparent,
        }
    }

    #[inline(always)]
    pub fn get_calligraphic_priority(c: char) -> u8 {
        match c {
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

    pub fn find_kashida_opportunities(text: &str) -> Vec<KashidaCandidate> {
        let chars: Vec<(usize, char)> = text.char_indices().collect();
        if chars.len() < 2 {
            return Vec::new();
        }

        let mut candidates = Vec::with_capacity(chars.len() / 2);

        for i in 0..chars.len() - 1 {
            let (idx, current) = chars[i];
            let (_, next) = chars[i + 1];

            let curr_type = Self::get_joining_type(current);
            let next_type = Self::get_joining_type(next);

            if curr_type == ArabicJoiningType::DualJoining 
                && (next_type == ArabicJoiningType::DualJoining || next_type == ArabicJoiningType::RightJoining)
            {
                let priority = Self::get_calligraphic_priority(current);
                candidates.push(KashidaCandidate {
                    char_index: idx,
                    unicode_char: current,
                    priority,
                    recommended_length_ratio: 1.0,
                });
            }
        }

        candidates.sort_unstable_by(|a, b| b.priority.cmp(&a.priority));
        candidates
    }

    pub fn apply_smart_kashida(text: &str, stretch_level: u8, target_char_index: Option<usize>) -> String {
        if stretch_level == 0 || text.is_empty() {
            return text.to_string();
        }

        let candidates = Self::find_kashida_opportunities(text);
        if candidates.is_empty() {
            return text.to_string();
        }

        // Exact memory pre-allocation to prevent heap re-allocations
        let tatweel_bytes = "ـ".len() * (stretch_level as usize);
        let mut result = String::with_capacity(text.len() + tatweel_bytes * candidates.len().min(2));

        let best_candidate_idx = candidates.first().map(|c| c.char_index);

        for (idx, c) in text.char_indices() {
            result.push(c);

            let should_elongate = if let Some(target) = target_char_index {
                idx == target
            } else {
                best_candidate_idx == Some(idx)
            };

            if should_elongate {
                for _ in 0..stretch_level {
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
    fn test_kashida_precision() {
        let text = "أثر";
        let candidates = KashidaEngine::find_kashida_opportunities(text);
        // 'ث' connects to 'ر'
        assert!(!candidates.is_empty());
        let stretched = KashidaEngine::apply_smart_kashida(text, 3, None);
        assert!(stretched.contains('ـ'));
        assert_eq!(stretched.chars().filter(|&c| c == 'ـ').count(), 3);
    }
}
