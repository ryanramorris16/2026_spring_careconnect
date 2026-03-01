#!/bin/bash

# Load .env and .env.local into the current shell session

ENV_FILES=(".env" ".env.local")

for ENV_FILE in "${ENV_FILES[@]}"; do
  if [ -f "$ENV_FILE" ]; then
    echo "Loading environment variables from $ENV_FILE"
    export $(grep -v '^#' "$ENV_FILE" | grep -v '^\s*$' | xargs)
  fi
done

# Set defaults if not provided
FLUTTER_WEB_PORT=${FLUTTER_WEB_PORT:-50030}
FLUTTER_DEVICE=${FLUTTER_DEVICE:-chrome}
CC_SENTIMENT_MODE=${CC_SENTIMENT_MODE:-balanced}

echo "Using FLUTTER_WEB_PORT=$FLUTTER_WEB_PORT"
echo "Using FLUTTER_DEVICE=$FLUTTER_DEVICE"
echo "Using CC_SENTIMENT_MODE=$CC_SENTIMENT_MODE"

# Run Flutter with the specified device and port
flutter run -d "$FLUTTER_DEVICE" --web-port="$FLUTTER_WEB_PORT" --dart-define=CARECONNECT_SENTIMENT_MODE="$CC_SENTIMENT_MODE"