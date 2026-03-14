#!/bin/bash

# --- 1. Load environment variables from .env and .env.local ---
ENV_FILES=(".env" ".env.local")

for ENV_FILE in "${ENV_FILES[@]}"; do
  if [ -f "$ENV_FILE" ]; then
    echo "Loading environment variables from $ENV_FILE"
    # Exporting while ignoring comments and empty lines
    export $(grep -v '^#' "$ENV_FILE" | grep -v '^\s*$' | xargs)
  fi
done

# Set defaults if not provided in the .env files
FLUTTER_WEB_PORT=${FLUTTER_WEB_PORT:-50030}
CC_SENTIMENT_MODE=${CC_SENTIMENT_MODE:-balanced}

echo "-------------------------------------"
echo "  CareConnect Frontend Startup"
echo "-------------------------------------"
echo "Using CC_SENTIMENT_MODE=$CC_SENTIMENT_MODE"

# --- 2. Interactive Menu Setup ---
PS3="Selection (enter number): "

select opt in "Android" "iOS" "Web" "Clean & Rebuild" "Quit"
do
    case $REPLY in
        1)
            echo "Launching on Android Emulator..."
            ANDROID_URL=$(grep CC_BASE_URL_ANDROID .env | cut -d '=' -f2)

            flutter run -d emulator \
            --dart-define-from-file=.env \
            --dart-define=BACKEND_URL=$ANDROID_URL \
            --dart-define=CARECONNECT_SENTIMENT_MODE=$CC_SENTIMENT_MODE
            break
            ;;
        2)
            echo "Launching on iOS Simulator..."
            if [ -d "ios" ]; then
                echo "Syncing CocoaPods..."
                (cd ios && pod install)
            fi
            flutter run -d ios \
            --dart-define-from-file=.env \
            --dart-define=CARECONNECT_SENTIMENT_MODE=$CC_SENTIMENT_MODE
            break
            ;;
        3)
            echo "Launching on Web (Chrome)..."
            WEB_URL=$(grep CC_BASE_URL_WEB .env | cut -d '=' -f2)

            flutter run -d chrome \
            --web-port=$FLUTTER_WEB_PORT \
            --dart-define-from-file=.env \
            --dart-define=BACKEND_URL=$WEB_URL \
            --dart-define=CARECONNECT_SENTIMENT_MODE=$CC_SENTIMENT_MODE
            ;;
        4)
            echo "Deep Cleaning Project..."
            flutter clean
            flutter pub get
            dart run build_runner build --delete-conflicting-outputs
            echo "Clean complete. Please run ./startup.sh again to launch."
            exit
            ;;
        5)
            echo "Exiting."
            exit
            ;;
        *)
            echo "Invalid option '$REPLY'. Please enter a number 1-5."
            ;;
    esac
done
