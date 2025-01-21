# 📱App Recommender

This project revolves around the development of an innovative Android application designed to enhance user convenience and personalization. The app leverages machine learning algorithms to provide intelligent recommendations tailored to the user’s usage history and specific time context. To further streamline the user experience, the application integrates a dynamic widget, ensuring quick and seamless access to suggestions.

## Table of Contents

- [About the Project](#about-the-project)
  - [Built With](#built-with)
- [Getting Started](#getting-started)
- [Usage](#usage)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [License](#license)
- [Contact](#contact)
- [Acknowledgements](#acknowledgements)

## About the Project

The project, aptly named App-Recommender, offers a smart and intuitive solution for Android users by predicting which application they are most likely to use next. Instead of searching through a crowded app list, users can rely on timely and relevant recommendations to save effort and boost efficiency.

For instance, if someone is using a finance management app, the system might suggest opening a calculator next. Similarly, when engaging with a social networking app, it might anticipate the need for another similar app. But something doesn't feel right!!🤔

To get these recommendations, would users need to open a separate app? That’s counterproductive and inconvenient.

Here’s where widgets come to the rescue! The App-Recommender integrates dynamic widgets right on the home screen, delivering personalized app suggestions in real time without requiring users to open any additional app. These widgets automatically update based on user behavior and context, ensuring that the right app is always just a tap away. This seamless, efficient approach makes life simpler, faster, and smarter for Android users.


### Built With
Majorly involves:
- [Android Studio](https://developer.android.com/studio/)
- [Kotlin](https://kotlinlang.org/)
- [xml](https://developer.android.com/guide/topics/resources/layout-resource)

## Getting Started
To get started with the App-Recommender project, follow these simple steps to set up your development environment and begin building your application:

**1. Install Android Studio**

   Begin by downloading and installing Android Studio on your computer. This is the official IDE for Android development and comes with everything you need to create and     test your app.

**2. Set Up Prerequisites**
   
  Ensure that you have the required SDKs and Java Development Kit (JDK) installed. If not, Android Studio will guide you through the process during installation. Verify that the correct versions of the SDK and JDK are configured for your project by checking the prerequisites in the Android Studio setup guide. I have used jdk 11, compile sdk 34 with min sdk 31, for this project.

**3. Create a New Project**

  - Open Android Studio and select Create New Project.
  - Choose a Blank Activity template to serve as the starting point for your application.
  - Set the project name, package name, and desired API level.

**4. Design the User Interface**
- Begin by adding UI elements like buttons and ImageViews to display the recommended apps.
- Use the Layout Editor to drag and drop components, or edit the XML file directly for finer control.

**5. Code the Backend and link Frontend**
  - Open the kotlin file for the layout and link the xml file to backend kotlin file,
  - In kotlin file I have worker class to perform the background calculation os cosine similarity.
  - I have also added some logicality for reusing the worker class in widget class and updating the widget. It may be different for any one else.

The project will primarily consist of a single activity to handle the main app logic.
Create additional classes as needed for specific functionalities, such as recommendation algorithms and data handling.
Include a dedicated Widget Class to manage the dynamic widget functionality for displaying app recommendations on the home screen.
With these steps, you’re ready to start building and customizing the App-Recommender project.

