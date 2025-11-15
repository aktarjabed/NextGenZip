# NextGenZip — Cloud AI Integration (Optional)

This README covers the optional Cloud AI integration added to NextGenZip.

## Summary

- Cloud AI is **optional** and **opt-in**.
- API keys are stored securely using `EncryptedSharedPreferences` / Android Keystore (AES-256-GCM).
- Default: Local AI (no network or paid API required).
- Providers supported (out of box): `openai`, `custom`. Placeholders exist for `gemini` and `deepseek`.

## Files added / changed

- `App.kt` — initializes stores
- `ApiKeyStore.kt` — encrypted key storage
- `CloudConfigStore.kt` — provider + flags storage
- `CloudUsageStore.kt` — local usage metrics
- `CloudAIClient.kt` — OkHttp-based provider client
- `AdvancedSettingsScreen.kt` — settings with cloud controls & help
- `AIChatScreenWithCloud.kt` — chat screen with cloud/local fallback
- `CloudUsageDashboard.kt`, `TroubleshootingScreen.kt`, `CloudHelpDialog.kt` — supportive UI

## Developer notes

1. **Dependencies**: add OkHttp, Coroutines, AndroidX Security.
2. **Permission**: add `<uses-permission android:name="android.permission.INTERNET" />`.
3. **Initialize**: `ApiKeyStore.init(context)` and `CloudConfigStore.init(context)` are called in `App.onCreate()`.
4. **Providers**:
   - **OpenAI**: uses `v1/chat/completions` shape. Model defaults to `gpt-4o-mini`.
   - **Custom**: expects POST JSON `{ "prompt": "..." }` and response `{"text":"..."}` or raw text.
   - **Gemini/DeepSeek**: placeholders — implement provider-specific request/response shape.
5. **Security**:
   - Keys are encrypted; do NOT log API keys.
   - Cloud usage stored locally is minimal and non-sensitive.

## UX flow

1. User enables Cloud AI in Settings and pastes API key.
2. User can test connectivity.
3. In AI Chat, if cloud is enabled and key present, app attempts cloud call and falls back to local on error.
4. Usage dashboard shows last latency and call count.

## Troubleshooting

- If cloud fails: open Troubleshooter in Settings → Troubleshooting.
- Check network connectivity, API key correctness, provider endpoint.

## Extending

- Add token accounting: parse provider response `usage` field and sum tokens in `CloudUsageStore`.
- Add billing: store token cost estimate per provider and show in Dashboard.
