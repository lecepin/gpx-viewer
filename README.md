# English README

# 🏃 GPX Track Visualization Tool

Convert your sports tracks (running, cycling, swimming, etc.) into beautiful 3:4 ratio sharing cards, easily export high-resolution transparent PNG images.

## ✨ Core Features

### 📁 GPX File Parsing
- One-click GPX file upload
- Auto-parse track points, time, distance and other data
- Support standard GPX format exported from various sports apps

### 🗺️ Map Visualization
- **Amap Rendering** - Fast access speed in China, clear maps
- **Satellite Imagery** - Enabled by default, shows real terrain
- **Path Drawing** - Custom colors (6 presets + free selection)
- **Smart Markers** 
  - Start point (green dot)
  - End point (red dot)
  - Every 1km marker (numbered circles)

### 🎨 Visualization Customization
- **Activity Type** - Custom input (open water swimming, morning run, cycling, etc.)
- **Path Color** - Color wheel selector + 6 quick presets
- **Layer Toggle** - Standard map / Satellite imagery / Traffic info

### 📸 High-Resolution Image Export
- **3:4 Golden Ratio** - Perfect for social media sharing
- **Transparent Background** - PNG format, transparent outside rounded corners
- **Multi-Resolution** - 1x / 2x / 3x / 4x (up to 3000×4000px)
- **Data Overlay** - Top activity info + Bottom sports data
- **Beautiful Layout** - Gradient mask + Frosted glass effect

### 📱 Responsive Design
- **Large Screen (>900px)** - Left-right layout, control panel + preview area
- **Small Screen (≤900px)** - Top-bottom layout, perfect for mobile
- **Strict 3:4 Ratio** - Auto-calculated, maintains perfect proportion

## How to Use

#### 📤 Upload GPX
1. Click "**Select GPX File**" button
2. Choose your GPX file (supports exports from Keep, Codoon, Strava, etc.)
3. Map automatically renders the path

#### 🎨 Customize Style
1. Modify "**Activity Type**" (e.g., Open Water Swimming)
2. Select "**Path Color**" (click presets or use color wheel)
3. Toggle "**Satellite Layer**" to view terrain

#### 📸 Export Image
1. Adjust map view to ideal position
2. Select export scale (**Recommended 2x**)
3. Click "**Export Image**"
4. Wait 1-2 seconds, PNG file downloads automatically

**Export File Example:** `GPX-2025-10-07-2x.png`

## 📦 Tech Stack

| Technology | Version | Description |
|------------|---------|-------------|
| **Vue 3** | ^3.4.0 | Composition API + Reactive System |
| **Amap JS API** | 2.0 | Map rendering and interaction |
| **html2canvas** | ^1.4.1 | DOM to image export |
| **fast-xml-parser** | ^4.3.0 | GPX file parsing |
| **Vite** | ^5.0.0 | Lightning fast build tool |
| **pnpm** | - | Efficient package manager |

## 🎯 Project Highlights

### 1️⃣ Smart Kilometer Markers
- Auto-calculate cumulative track distance
- Precise markers every 1 kilometer
- Numbered circles, clear and readable
- Click markers to show details

### 2️⃣ 3:4 Golden Ratio
- Fits mainstream social platforms
- Auto-calculate optimal dimensions
- Responsive adaptive
- Always maintains perfect proportion

### 3️⃣ Transparent PNG Export
- Transparent outside rounded corners
- Complete Alpha channel
- Can overlay any background
- Suitable for secondary design

### 4️⃣ Satellite Map Priority
- Satellite imagery enabled by default
- Shows real terrain
- Clear oceans and mountains
- Perfect for outdoor sports tracks

### 5️⃣ Frosted Glass Aesthetics
- Top gradient mask
- Bottom frosted effect
- iOS-style design
- Clear readable data

<img width="890" height="811" alt="image" src="https://github.com/user-attachments/assets/d53ebfeb-396b-4a74-80d0-c6a79c3b7365" />

