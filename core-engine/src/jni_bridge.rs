//! JNI Bridge exposing Athar Core Rust engine to Android Kotlin runtime

use jni::objects::{JClass, JString};
use jni::sys::{jint, jlong, jstring};
use jni::JNIEnv;

use crate::kashida::KashidaEngine;

const ENGINE_VERSION: &str = "1.0.0-arm64";

#[no_mangle]
pub extern "system" fn Java_app_xrtm8_athar_core_AtharNativeEngine_nativeGetVersion<'local>(
    env: JNIEnv<'local>,
    _class: JClass<'local>,
) -> jstring {
    let output = env.new_string(ENGINE_VERSION).expect("Couldn't create java string");
    output.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_app_xrtm8_athar_core_AtharNativeEngine_nativeApplyKashida<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    input_text: JString<'local>,
    stretch_level: jint,
    target_index: jlong,
) -> jstring {
    let text: String = match env.get_string(&input_text) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("").unwrap().into_raw(),
    };

    let target_idx = if target_index >= 0 {
        Some(target_index as usize)
    } else {
        None
    };

    let result = KashidaEngine::apply_smart_kashida(&text, stretch_level.clamp(0, 10) as u8, target_idx);

    let output = env.new_string(result).expect("Couldn't create java string");
    output.into_raw()
}

#[no_mangle]
pub extern "system" fn Java_app_xrtm8_athar_core_AtharNativeEngine_nativeFindOpportunitiesJson<'local>(
    mut env: JNIEnv<'local>,
    _class: JClass<'local>,
    input_text: JString<'local>,
) -> jstring {
    let text: String = match env.get_string(&input_text) {
        Ok(s) => s.into(),
        Err(_) => return env.new_string("[]").unwrap().into_raw(),
    };

    let candidates = KashidaEngine::find_kashida_opportunities(&text);
    let json_str = serde_json::to_string(&candidates).unwrap_or_else(|_| "[]".to_string());

    let output = env.new_string(json_str).expect("Couldn't create java string");
    output.into_raw()
}
