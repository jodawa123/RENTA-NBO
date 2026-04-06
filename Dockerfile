# Stage 1: Build the Android APK
FROM ubuntu:22.04 AS builder

# Install build dependencies
RUN apt-get update && apt-get install -y \
    openjdk-17-jdk \
    git \
    wget \
    unzip \
    && rm -rf /var/lib/apt/lists/*

# Install Android SDK
ENV ANDROID_SDK_ROOT=/opt/android-sdk
ENV PATH=$PATH:$ANDROID_SDK_ROOT/cmdline-tools/latest/bin:$ANDROID_SDK_ROOT/platform-tools

RUN mkdir -p $ANDROID_SDK_ROOT/cmdline-tools \
    && wget -q https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip \
    && unzip commandlinetools-linux-11076708_latest.zip -d $ANDROID_SDK_ROOT/cmdline-tools \
    && mv $ANDROID_SDK_ROOT/cmdline-tools/cmdline-tools $ANDROID_SDK_ROOT/cmdline-tools/latest \
    && rm commandlinetools-linux-11076708_latest.zip

# Accept Android licenses
RUN yes | $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager --licenses

# Install SDK components
RUN $ANDROID_SDK_ROOT/cmdline-tools/latest/bin/sdkmanager \
    "platform-tools" \
    "platforms;android-34" \
    "build-tools;34.0.0"

# Copy project
WORKDIR /app
COPY . .

# Create local.properties with API key
ARG MAPS_API_KEY
RUN echo "MAPS_API_KEY=$MAPS_API_KEY" > local.properties

# Build APK
RUN chmod +x gradlew
RUN ./gradlew assembleDebug --no-daemon

# Stage 2: Serve APK
FROM nginx:alpine

COPY --from=builder /app/app/build/outputs/apk/debug/app-debug.apk /usr/share/nginx/html/

RUN echo '<html><head><title>Rentanbo Download</title></head><body>' > /usr/share/nginx/html/index.html
RUN echo '<h1>Rentanbo App</h1>' >> /usr/share/nginx/html/index.html
RUN echo '<a href="/app-debug.apk">Download APK</a>' >> /usr/share/nginx/html/index.html
RUN echo '</body></html>' >> /usr/share/nginx/html/index.html

EXPOSE 80
CMD ["nginx", "-g", "daemon off;"]