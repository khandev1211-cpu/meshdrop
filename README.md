# MeshDrop 🚀🔒

![Android](https://img.shields.io/badge/Platform-Android-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?logo=kotlin&logoColor=white)
![Architecture](https://img.shields.io/badge/Architecture-MVVM-blue)
![License](https://img.shields.io/badge/License-MIT-orange)

**MeshDrop** is a lightning-fast, offline-first, peer-to-peer (P2P) file transfer application for Android. It allows users to send massive files directly between devices without internet access, cellular data, or Bluetooth pairing. 

Built with low-level socket programming and Android's Wi-Fi Direct APIs, MeshDrop establishes a high-bandwidth local connection and secures every byte with AES-256 End-to-End Encryption.

> **Repository**: [github.com/khandev1211-cpu/MeshDrop](https://github.com/khandev1211-cpu/MeshDrop)  
> **Author**: Irfan Khan

---

## 📱 Screenshots

*(Add your UI screenshots here. E.g., Radar Discovery Screen, Active Transfer Progress, File History)*

<div align="center">
  <img src="https://via.placeholder.com/250x500.png?text=Discovery+Screen" width="200" />
  <img src="https://via.placeholder.com/250x500.png?text=Transfer+Progress" width="200" />
  <img src="https://via.placeholder.com/250x500.png?text=File+History" width="200" />
</div>

---

## ✨ Key Features

- 📡 **100% Offline**: No cloud servers, no cellular data. Transfers happen over a local Wi-Fi Direct ad-hoc network.
- 🚀 **High Speed**: Utilizes Wi-Fi bandwidth (up to 250+ Mbps), making it significantly faster than standard Bluetooth transfers.
- 🔒 **End-to-End Encryption**: Implements ECDH (Elliptic Curve Diffie-Hellman) for secure key exchange and AES-256-GCM for packet encryption.
- 📦 **Smart File Chunking**: Capable of sending massive files (>5GB) by breaking data into manageable memory chunks, preventing `OutOfMemory` (OOM) crashes.
- 🎨 **Modern UI**: Built entirely with **Jetpack Compose** for a fluid, reactive user interface.
- 📂 **Any File Type**: Send APKs, directories, raw binaries, 4K videos, or multiple files at once.

---

## 🛠 Architecture & Under the Hood

MeshDrop bypasses high-level HTTP libraries and works directly with OS-level networking and hardware APIs. 

### 1. Discovery & Connection (`WifiP2pManager`)
The app uses Android's `WifiP2pManager` to broadcast its presence. When Device A wants to connect to Device B, Android negotiates a Group Owner (GO). The GO acts as a localized DHCP server, assigning an IP address to the client device, effectively creating a 2-node private LAN.

### 2. The Cryptographic Handshake
Before any files are transferred, the two devices establish a secure tunnel over a raw TCP socket:
1. Both devices generate an ephemeral ECDH public/private key pair.
2. They exchange public keys over the socket.
3. Both derive the identical shared secret.
4. The shared secret is hashed (HKDF) to generate the **AES-256 Symmetric Key**.

### 3. I/O Stream & File Chunking
Reading a 2GB video file into RAM will instantly crash an Android app. MeshDrop utilizes **Buffered Streams** and **Coroutines**:
* **Sender**: Opens a `FileInputStream`, reads the file in `8KB` or `64KB` chunks, encrypts the chunk on the fly, and pushes it to the `DataOutputStream` over the TCP socket.
* **Receiver**: Listens on the `ServerSocket`, decrypts incoming chunks on the fly, and writes them directly to disk using Android's `MediaStore` or Storage Access Framework (SAF).

---

## 💻 Tech Stack

- **Language**: Kotlin
- **UI Framework**: Jetpack Compose, Material 3
- **Concurrency**: Kotlin Coroutines & Asynchronous Flows
- **Networking**: `java.net.Socket`, `java.net.ServerSocket`, `android.net.wifi.p2p.*`
- **Security**: Java Cryptography Architecture (JCA) / Google Tink
- **Local Database**: Room (SQLite) for storing transfer history
- **Architecture Pattern**: MVVM (Model-View-ViewModel) with Clean Architecture principles

---

## 🚀 Building from Source

### Prerequisites
- **Android Studio**: Jellyfish (2023.3.1) or newer
- **Android SDK**: API Level 34
- **Minimum SDK**: API Level 26 (Android 8.0)
- **Physical Devices**: You need **two physical Android devices** to test Wi-Fi Direct (the Android Emulator does not support Wi-Fi P2P connections).

### Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/khandev1211-cpu/MeshDrop.git
