# Example API Requests

## Prerequisites
```bash
# Set your API endpoint
export API_URL="https://nasa-mirror-api.yourusername.workers.dev"
# Or for local development:
export API_URL="http://localhost:8787"
```

## Get Latest APOD

### Basic Request
```bash
curl -s "${API_URL}/apod/latest" | jq .
```

### With ETag Caching
```bash
# First request - get ETag
RESPONSE=$(curl -si "${API_URL}/apod/latest")
ETAG=$(echo "$RESPONSE" | grep -i "etag:" | cut -d' ' -f2 | tr -d '\r')

echo "ETag: $ETAG"

# Subsequent request with If-None-Match
curl -i -H "If-None-Match: $ETAG" "${API_URL}/apod/latest"
# Should return 304 Not Modified if content unchanged
```

### With Last-Modified
```bash
# First request
RESPONSE=$(curl -si "${API_URL}/apod/latest")
LAST_MODIFIED=$(echo "$RESPONSE" | grep -i "last-modified:" | cut -d' ' -f2- | tr -d '\r')

echo "Last-Modified: $LAST_MODIFIED"

# Subsequent request
curl -i -H "If-Modified-Since: $LAST_MODIFIED" "${API_URL}/apod/latest"
```

## Get APOD by Date

### Specific Date
```bash
curl -s "${API_URL}/apod/2024-10-01" | jq .
```

### Invalid Date (400 Error)
```bash
curl -s "${API_URL}/apod/2024-13-01" | jq .
# {
#   "error": "Invalid date format: 2024-13-01. Expected YYYY-MM-DD",
#   "status": 400
# }
```

### Non-existent Date (404 Error)
```bash
curl -s "${API_URL}/apod/1990-01-01" | jq .
# {
#   "error": "APOD entry not found for date: 1990-01-01",
#   "status": 404
# }
```

## Rate Limiting

### Trigger Rate Limit
```bash
# Send 101 requests quickly (assuming limit is 100/min)
for i in {1..101}; do
  curl -s "${API_URL}/apod/latest" -w "\n%{http_code}\n"
  sleep 0.1
done
# Last request should return 429 Too Many Requests
```

## Response Headers

### Check Security Headers
```bash
curl -I "${API_URL}/apod/latest"
```

Expected headers:
```
HTTP/2 200
content-type: application/json
etag: a1b2c3d4e5f6...
cache-control: public, max-age=3600
last-modified: Sat, 05 Oct 2024 12:00:00 GMT
content-security-policy: default-src 'none'; img-src *; ...
strict-transport-security: max-age=31536000; includeSubDomains; preload
x-content-type-options: nosniff
x-frame-options: DENY
referrer-policy: strict-origin-when-cross-origin
access-control-allow-origin: *
```

## JavaScript Examples

### Fetch Latest with Error Handling
```javascript
async function fetchLatestAPOD() {
  const API_URL = 'https://nasa-mirror-api.yourusername.workers.dev';
  
  try {
    const response = await fetch(`${API_URL}/apod/latest`);
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(`API Error ${error.status}: ${error.error}`);
    }
    
    const apod = await response.json();
    
    console.log(`Title: ${apod.title}`);
    console.log(`Date: ${apod.date}`);
    console.log(`Media Type: ${apod.media_type}`);
    
    if (apod.media_type === 'image') {
      console.log(`Image URL: ${apod.url}`);
      console.log(`HD URL: ${apod.hdurl || 'N/A'}`);
    } else {
      console.log(`Video URL: ${apod.url}`);
      console.log(`Thumbnail: ${apod.thumbnail || 'N/A'}`);
    }
    
    return apod;
  } catch (error) {
    console.error('Failed to fetch APOD:', error);
    throw error;
  }
}

// Usage
fetchLatestAPOD().then(apod => {
  // Use the data...
});
```

### Fetch with Caching
```javascript
class APODClient {
  constructor(baseURL) {
    this.baseURL = baseURL;
    this.cache = new Map();
  }
  
  async fetchLatest() {
    const cacheKey = 'latest';
    const cached = this.cache.get(cacheKey);
    
    const headers = {};
    if (cached?.etag) {
      headers['If-None-Match'] = cached.etag;
    }
    
    const response = await fetch(`${this.baseURL}/apod/latest`, { headers });
    
    if (response.status === 304) {
      console.log('Using cached data (304 Not Modified)');
      return cached.data;
    }
    
    const data = await response.json();
    const etag = response.headers.get('etag');
    
    this.cache.set(cacheKey, { data, etag });
    
    return data;
  }
  
  async fetchByDate(date) {
    const cacheKey = `date:${date}`;
    const cached = this.cache.get(cacheKey);
    
    if (cached) {
      return cached.data;
    }
    
    const response = await fetch(`${this.baseURL}/apod/${date}`);
    
    if (!response.ok) {
      const error = await response.json();
      throw new Error(error.error);
    }
    
    const data = await response.json();
    this.cache.set(cacheKey, { data });
    
    return data;
  }
}

// Usage
const client = new APODClient('https://nasa-mirror-api.yourusername.workers.dev');

client.fetchLatest().then(apod => console.log(apod));
client.fetchByDate('2024-10-01').then(apod => console.log(apod));
```

### Display in HTML
```html
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <title>APOD Viewer</title>
  <style>
    body { font-family: Arial, sans-serif; max-width: 800px; margin: 0 auto; padding: 20px; }
    img { max-width: 100%; height: auto; }
    .error { color: red; }
  </style>
</head>
<body>
  <h1>Astronomy Picture of the Day</h1>
  <div id="apod-container"></div>
  
  <script>
    const API_URL = 'https://nasa-mirror-api.yourusername.workers.dev';
    
    async function displayAPOD() {
      const container = document.getElementById('apod-container');
      
      try {
        const response = await fetch(`${API_URL}/apod/latest`);
        const apod = await response.json();
        
        if (apod.error) {
          container.innerHTML = `<p class="error">Error: ${apod.error}</p>`;
          return;
        }
        
        let mediaHTML = '';
        if (apod.media_type === 'image') {
          mediaHTML = `
            <a href="${apod.hdurl || apod.url}" target="_blank">
              <img src="${apod.url}" alt="${apod.title}">
            </a>
          `;
        } else {
          // Video (iframe)
          mediaHTML = `
            <iframe width="100%" height="450" src="${apod.url}" 
                    frameborder="0" allowfullscreen></iframe>
          `;
        }
        
        container.innerHTML = `
          <h2>${apod.title}</h2>
          <p><strong>Date:</strong> ${apod.date}</p>
          ${mediaHTML}
          <div>${apod.explanation}</div>
          ${apod.copyright ? `<p><em>${apod.copyright}</em></p>` : ''}
          <p><small><a href="${apod.source_url}" target="_blank">View Original</a></small></p>
        `;
      } catch (error) {
        container.innerHTML = `<p class="error">Failed to load APOD: ${error.message}</p>`;
      }
    }
    
    displayAPOD();
  </script>
</body>
</html>
```

## Python Examples

### Basic Request
```python
import requests
from datetime import datetime

API_URL = "https://nasa-mirror-api.yourusername.workers.dev"

def get_latest_apod():
    response = requests.get(f"{API_URL}/apod/latest")
    response.raise_for_status()
    return response.json()

def get_apod_by_date(date):
    """
    Args:
        date: datetime object or string in YYYY-MM-DD format
    """
    if isinstance(date, datetime):
        date = date.strftime("%Y-%m-%d")
    
    response = requests.get(f"{API_URL}/apod/{date}")
    response.raise_for_status()
    return response.json()

# Usage
apod = get_latest_apod()
print(f"Title: {apod['title']}")
print(f"Date: {apod['date']}")
print(f"URL: {apod['url']}")

# Get specific date
apod_oct1 = get_apod_by_date("2024-10-01")
```

### With Caching
```python
import requests
from datetime import datetime
import hashlib

class APODClient:
    def __init__(self, base_url):
        self.base_url = base_url
        self.session = requests.Session()
        self.cache = {}
    
    def get_latest(self):
        cache_key = "latest"
        
        headers = {}
        if cache_key in self.cache:
            headers["If-None-Match"] = self.cache[cache_key]["etag"]
        
        response = self.session.get(
            f"{self.base_url}/apod/latest",
            headers=headers
        )
        
        if response.status_code == 304:
            print("Using cached data")
            return self.cache[cache_key]["data"]
        
        response.raise_for_status()
        data = response.json()
        
        etag = response.headers.get("etag")
        if etag:
            self.cache[cache_key] = {"data": data, "etag": etag}
        
        return data
    
    def get_by_date(self, date):
        if isinstance(date, datetime):
            date = date.strftime("%Y-%m-%d")
        
        response = self.session.get(f"{self.base_url}/apod/{date}")
        response.raise_for_status()
        return response.json()

# Usage
client = APODClient("https://nasa-mirror-api.yourusername.workers.dev")
apod = client.get_latest()
```

## Testing with Newman (Postman CLI)

Create a Postman collection `apod-tests.json`:
```json
{
  "info": {
    "name": "NASA Mirror API Tests",
    "schema": "https://schema.getpostman.com/json/collection/v2.1.0/collection.json"
  },
  "item": [
    {
      "name": "Get Latest APOD",
      "request": {
        "method": "GET",
        "url": "{{baseUrl}}/apod/latest"
      },
      "event": [{
        "listen": "test",
        "script": {
          "exec": [
            "pm.test('Status is 200', () => pm.response.to.have.status(200));",
            "pm.test('Response has date field', () => pm.response.to.have.jsonBody('date'));",
            "pm.test('Response has title field', () => pm.response.to.have.jsonBody('title'));"
          ]
        }
      }]
    }
  ]
}
```

Run tests:
```bash
newman run apod-tests.json --env-var "baseUrl=https://nasa-mirror-api.yourusername.workers.dev"
```

## Load Testing with Apache Bench

```bash
# Test 1000 requests with concurrency of 10
ab -n 1000 -c 10 https://nasa-mirror-api.yourusername.workers.dev/apod/latest

# With custom headers
ab -n 1000 -c 10 -H "Accept: application/json" \
   https://nasa-mirror-api.yourusername.workers.dev/apod/latest
```
