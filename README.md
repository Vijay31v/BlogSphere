# BlogSphere
BlogSphere is a modern, full-featured blogging platform built with **Spring Boot**, **Thymeleaf**, and **Bootstrap**. It supports real-time notifications, user management, content moderation, subscriptions, and a rich admin dashboard with analytics.

---

## Features

- 📝 **Rich Blogging:** Create, edit, and manage blog posts with HTML formatting support.
- 👤 **User Authentication:** Secure registration, login, password reset, and profile management.
- 🔔 **Real-Time Notifications:** WebSocket-powered notifications for comments, likes, follows, and more.
- 📊 **Admin Dashboard:** Analytics with dynamic charts, filters (year/month), and management tools for users, blogs, categories and comments.
- 💬 **Comments & Likes:** Engage with posts via threaded comments and likes.
- 👥 **Follow System:** Subscribe to bloggers and receive email notifications for new posts.
- 📧 **Email Integration:** Password reset, welcome, comment, and follow notifications sent via email.
- 🏷️ **Tag & Category Support:** Organize posts by tags and categories.
- 🛡️ **Role-Based Access:** Admin, author, and regular user roles with appropriate permissions.
- 📱 **Responsive Design:** Mobile-friendly UI with Bootstrap 5.

---

## Getting Started

### Prerequisites

- Java 17+
- Maven 3.8+
- MySQL or PostgreSQL (or update `application.properties` for your database)
- Node.js (optional, for asset building)

### Installation

1. **Clone the repository:**
    
    ```cmd
    git clone https://github.com/Vijay31v/BlogSphere.git
    cd blogsphere
    ```


2. **Configure the database:**

   Edit `src/main/resources/application.properties`:
    ```
    spring.datasource.url=jdbc:mysql://localhost:3306/blogsphere
    spring.datasource.username=your_db_user
    spring.datasource.password=your_db_password
    ```


4. **Configure Email:**
   
   Edit `src/main/resources/application.properties`:
   ```
     spring.mail.username=your_gmail
   ```
   
    #### Configuring Gmail SMTP with an App Password

    > **Important:** For Gmail SMTP, you must use a Google App Password, **not your regular Gmail password**.

      ##### Step 1: Enable 2-Step Verification

      1. Go to your [Google Account Security page](https://myaccount.google.com/security).
      2. Under "Signing in to Google", click **2-Step Verification**.
      3. Follow the steps to enable it (you may need to enter your phone number and verify it).
      
      ##### Step 2: Generate an App Password
      
      1. After enabling 2-Step Verification, return to the [Security](https://myaccount.google.com/security) section.
      2. Search for **App passwords** (you may need to sign in again).
         - Direct link: [https://myaccount.google.com/apppasswords](https://myaccount.google.com/apppasswords)
      3. Give your app any name  eg: **Mail**.
      4. Click **Create**.
      6. **Copy the 16-character app password** (no spaces). You will not be able to see it again after closing the dialog.
      
      ##### Step 3: Use the App Password in Your Spring Boot Application
      
      In your `application.properties`:
      
      
      - Replace `your-app-password-here` with the app password you generated (no spaces).
      - **Do NOT use your regular Gmail password.**
      
      #### Additional Tips
      
      - If you change your Google account password, all app passwords are revoked. You must generate a new app password.
      - If you don't see the App passwords option, ensure 2-Step Verification is fully enabled and you're not on a restricted (e.g., work/school) account.
      - Store your app password securely; treat it like any other sensitive credential.
      
      ---
      
      **References:**
      - [Google: Create & use app passwords](https://support.google.com/accounts/answer/185833)
      - [Spring Boot Email Guide](https://www.baeldung.com/spring-email)
      



5. **Build and run the application:**
    ```
    mvn spring-boot:run
    ```


7. **Access the app:**
    - [http://localhost:8080](http://localhost:8080)

---

## Usage

- **Register** a new user or log in as admin.
- **Create and manage blogs** from your dashboard.
- **Follow other bloggers** and receive notifications for new posts.
- **Admin users** can access `/admin` for analytics and management.

---

## Project Structure

  ```
  src/
  main/
  java/
  org.example.blogsphere/
  controller/
  entity/
  repository/
  service/
  config/
  resources/
  templates/
  static/
  application.properties
  ```


---

## Customization

- **Themes:** Modify `static/css/main.css` for custom styles.
- **Email Templates:** Edit files in `templates/email/`.
- **Notification Logic:** See `NotificationService` and WebSocket config.

---

## Contributing

Contributions, issues, and feature requests are welcome!  
Please open an [issue](https://github.com/Vijay31v/BlogSphere/issues) or submit a pull request.

---

## License

This project is licensed under the MIT License. See [LICENSE](LICENSE) for details.

---

## Acknowledgements

- [Spring Boot](https://spring.io/projects/spring-boot)
- [Thymeleaf](https://www.thymeleaf.org/)
- [Bootstrap](https://getbootstrap.com/)
- [CKEditor](https://ckeditor.com/)
- [Chart.js](https://www.chartjs.org/)
- [jsoup](https://jsoup.org/)
- [Highlight.js](https://highlightjs.org/)
  

---

**Enjoy blogging with BlogSphere!**
