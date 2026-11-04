<img width="316" height="741" alt="image" src="https://github.com/user-attachments/assets/e6abd991-a7b7-471f-ae43-d9d4465ba6da" /># 🛡️ FaceGuard – AI-Powered Android App Security

**FaceGuard** is an Android security application that uses facial recognition to help protect personal applications from unauthorized access.

The application allows users to enroll trusted individuals, recognize faces during authentication, and control access to selected applications based on the recognized identity.

Instead of relying only on traditional passwords or PINs, FaceGuard introduces a face-based access control mechanism.

## ✨ Key Features

- **Face Enrollment:** Register trusted individuals such as Owner, Family, and Friends.
- **Face Recognition:** Identify enrolled individuals using facial recognition.
- **App Protection:** Configure access rules for selected applications.
- **Profile Management:** Add, update, view, and delete enrolled face profiles.
- **Activity Logs:** Maintain records of recognition and application access activities.
- **Unknown Face Handling:** Identify unrecognized individuals and record relevant activity.
- **Access Control:** Use Android Accessibility Services to support application-level enforcement.

## ⚙️ How It Works

1. **Profile Enrollment:** The user registers a person's profile through the application.
2. **Face Validation:** The system checks the captured image for a valid face.
3. **Face Recognition:** The captured face is compared against enrolled profiles.
4. **Identity Verification:** The system determines whether the person matches an enrolled profile.
5. **App Access Control:** Configured application rules are used to determine access.
6. **Activity Logging:** Recognition and access-related activities are recorded.

## 🛠️ Technology Stack

| Component | Technology |
|---|---|
| Programming Language | Kotlin |
| UI Development | Jetpack Compose |
| Database | Room Database |
| Face Detection | Google ML Kit |
| Android Platform | Android SDK |
| Application Monitoring | Accessibility Service |
| Development Environment | Android Studio |
| Architecture | Repository Pattern |

## 🏗️ Core Modules

- **Profile Management:** Handles trusted user registration and profile management.
- **Face Recognition:** Handles face validation and identity matching.
- **App Rules:** Manages application-specific access permissions.
- **App Enforcement:** Uses Android Accessibility Service to monitor application transitions and support access restrictions.
- **Activity Logs:** Stores relevant application and recognition events.

## 📱 Application Screenshots

Screenshots of the application will be added here.

| Home Screen | Profile Management |
|---|---|
<img width="318" height="742" alt="image" src="https://github.com/user-attachments/assets/59073f60-a2b1-4b6b-a0f2-ecb9a0bfc0d7" />
<img width="316" height="741" alt="image" src="https://github.com/user-attachments/assets/ac0929d1-adbb-4504-906d-952cd2e3d263" />


| Face Recognition 
|---|---|
<img width="317" height="744" alt="image" src="https://github.com/user-attachments/assets/e8cf2f53-2bb7-4250-ad0f-f7f388ba24e5" />


## 🚀 Getting Started

### Prerequisites

- Android Studio
- Android SDK
- Kotlin support
- An Android device or emulator

### Installation

**1. Clone the repository**

```bash
git clone https://github.com/karthikreddyg13-blip/faceguard.git
```

**2. Open the project**

Open the cloned project in Android Studio.

**3. Sync Gradle**

Allow Android Studio to download the required dependencies and synchronize the project.

**4. Connect an Android device**

Enable Developer Options and USB Debugging on your Android device.

**5. Run the application**

Select the application configuration and click the Run button in Android Studio.

### Required Permissions

Depending on the enabled functionality, FaceGuard may require:

- Camera permission – For face enrollment and recognition.
- Accessibility permission – For application monitoring and access enforcement.

Accessibility access must be explicitly enabled by the device user through Android Settings.

## 🔐 Privacy & Security

FaceGuard is designed around user-controlled identity profiles and application access rules.

- Users manage their own enrolled profiles.
- Recognition and access-related activities are maintained in the application's local database.
- Camera and Accessibility permissions are requested for their respective functionalities.

**Note:** Users should only enroll individuals with their knowledge and consent.

## 🔮 Future Improvements

- Improved liveness detection to reduce photo-based spoofing.
- Enhanced recognition accuracy under different lighting conditions.
- Improved recognition performance and resource optimization.
- More granular application access controls.
- Enhanced security and privacy mechanisms.
- Better compatibility across Android devices and versions.

## 📌 Project Status

FaceGuard is an ongoing development project. Core profile management, face validation, recognition, application rules, and activity logging have been implemented. Application enforcement and additional security improvements are undergoing further testing and refinement.

## 👨‍💻 Developer

**G. V. Karthik Reddy**

B.Tech – Computer Science and Engineering (AI & Data Science)

Koneru Lakshmaiah Education Foundation

- GitHub: [@karthikreddyg13-blip](https://github.com/karthikreddyg13-blip)
- Project Repository: [FaceGuard](https://github.com/karthikreddyg13-blip/faceguard)

---

⭐ If you find this project interesting, consider giving the repository a star!

*Built with Kotlin, Android, and AI.*
