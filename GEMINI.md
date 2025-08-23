# Gemini Workflow

This document outlines the development and testing workflow for this project when using the Gemini assistant.

## Development Workflow

1.  **Make Code Changes:** The Gemini assistant will make the necessary code changes based on the user's request.

2.  **Build the Application:** After making changes, the assistant will run the Gradle build command to ensure the code compiles without errors.

    ```bash
    ./gradlew build
    ```

3.  **Deploy for Testing:** Once the build is successful, the assistant will deploy the application to the user's connected device for testing.

    ```bash
    adb install -r app/build/outputs/apk/debug/app-debug.apk
    ```

## Testing

After the application is deployed, the user should test the changes on their device and provide feedback to the assistant.
