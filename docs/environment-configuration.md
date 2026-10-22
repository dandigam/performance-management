Local and production configuration
==================================

The same JAR supports both environments. Do not edit application.properties when deploying.

Local IDE runs default to the local profile and use application.properties directly.
A separate local secrets file is not required. Values in this tracked resource are packaged
in the JAR, including any credentials placed there.

Production: set SPRING_PROFILES_ACTIVE=prod, or launch:

    java -jar performance-management-system-0.0.1-SNAPSHOT.jar --spring.profiles.active=prod

Set these environment variables on the server: DB_URL, DB_USERNAME, DB_PASSWORD,
JWT_SECRET, APP_BASE_URL. Set MAIL_USERNAME, MAIL_PASSWORD, and MAIL_FROM for email.
Set DOCUMENT_STORAGE_PATH to the existing persistent document directory.
Keep the existing SECURE_COOKIE setting appropriate for your deployment's HTTP/HTTPS scheme.

Alternatively, create an external properties file on the server and set APP_CONFIG_FILE
to its absolute path. Use standard property names, for example:

    spring.datasource.url=jdbc:mysql://DATABASE_HOST:3306/DATABASE_NAME
    spring.datasource.username=DATABASE_USER
    spring.datasource.password=DATABASE_PASSWORD
    jwt.secret=YOUR_EXISTING_JWT_SECRET
    app.mail.base-url=https://YOUR_APPLICATION_HOST
    spring.mail.username=MAIL_USER
    spring.mail.password=MAIL_PASSWORD
    app.mail.from=MAIL_SENDER
    app.documents.storage-path=/path/to/existing/documents

Imported file properties override the profile's placeholders; environment variables can
also override Spring property names directly (for example SPRING_DATASOURCE_URL).
Never commit the server properties file. Keep existing production values when switching
profiles. Restart the application after changing environment variables or external settings.
Neither profile imports the local secrets file.
