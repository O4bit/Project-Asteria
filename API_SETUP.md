# 🔑 API Key Setup

Project Asteria uses NASA's Astronomy Picture of the Day (APOD) API. To run the app, you'll need to obtain a free API key.

## Step 1: Get NASA API Key
1. Visit [NASA API Portal](https://api.nasa.gov/)
2. Click "Generate API Key"
3. Fill out the form (it's free!)
4. Copy your API key

## Step 2: Configure Local Properties
1. Copy `local.properties.template` to `local.properties`
2. Replace `YOUR_NASA_API_KEY_HERE` with your actual NASA API key:

```properties
# API Keys
nasa.api.key=your_actual_api_key_here
```

## Step 3: Build & Run
The app will now use your API key automatically through `BuildConfig.NASA_API_KEY`.

## 🔒 Security Notes
- `local.properties` is automatically excluded from version control
- Never commit real API keys to public repositories
- The app will use `DEMO_KEY` as fallback (limited requests)

---
**⚠️ Important:** The NASA DEMO_KEY has rate limits. For best experience, use your own API key.
