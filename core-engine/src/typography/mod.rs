//! Typography module: High-performance Arabic text shaping & OpenType layout

use std::str::FromStr;
use rustybuzz::{Face, Feature, Language, UnicodeBuffer};
use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ShapedGlyph {
    pub glyph_id: u32,
    pub cluster: u32,
    pub x_advance: i32,
    pub y_advance: i32,
    pub x_offset: i32,
    pub y_offset: i32,
}

#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct ShapedTextResult {
    pub text: String,
    pub total_width: i32,
    pub glyphs: Vec<ShapedGlyph>,
}

pub struct TypographyEngine;

impl TypographyEngine {
    /// Shapes Arabic text using an OpenType font buffer with full BiDi & cursive features
    pub fn shape_arabic(font_data: &[u8], text: &str) -> Result<ShapedTextResult, String> {
        let face = Face::from_slice(font_data, 0)
            .ok_or_else(|| "Failed to parse OpenType font".to_string())?;

        let mut buffer = UnicodeBuffer::new();
        buffer.push_str(text);
        buffer.set_direction(rustybuzz::Direction::RightToLeft);
        buffer.set_script(rustybuzz::script::ARABIC);
        if let Ok(lang) = Language::from_str("ara") {
            buffer.set_language(lang);
        }

        // Enable all high-end Arabic typography features
        let features = [
            Feature::from_str("liga").unwrap(),
            Feature::from_str("rlig").unwrap(),
            Feature::from_str("calt").unwrap(),
            Feature::from_str("jalt").unwrap(),
            Feature::from_str("swsh").unwrap(),
            Feature::from_str("mkmk").unwrap(),
            Feature::from_str("mark").unwrap(),
        ];

        let glyph_buffer = rustybuzz::shape(&face, &features, buffer);
        let infos = glyph_buffer.glyph_infos();
        let positions = glyph_buffer.glyph_positions();

        let mut total_width = 0;
        let mut glyphs = Vec::with_capacity(infos.len());

        for (info, pos) in infos.iter().zip(positions.iter()) {
            total_width += pos.x_advance;
            glyphs.push(ShapedGlyph {
                glyph_id: info.glyph_id,
                cluster: info.cluster,
                x_advance: pos.x_advance,
                y_advance: pos.y_advance,
                x_offset: pos.x_offset,
                y_offset: pos.y_offset,
            });
        }

        Ok(ShapedTextResult {
            text: text.to_string(),
            total_width,
            glyphs,
        })
    }
}
