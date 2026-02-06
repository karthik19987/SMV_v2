# ShopKeeper Pro - Android POS App

A modern Point of Sale (POS) system for small shops built with Kotlin and Android Architecture Components.

## Features

### ✅ Implemented
- **User Authentication** - Multi-user login system
- **Dashboard** - Today's sales, expenses, and profit overview
- **Database Structure** - Room database with entities for Users, Items, Sales, and Expenses
- **Modern UI** - Material Design 3 with bottom navigation
- **MVVM Architecture** - Clean architecture with ViewModels and LiveData

### 🚧 In Development
- **Sales Calculator** - Calculator-like interface for recording sales
- **Expense Tracker** - Track expenses under different categories
- **Reports & Analytics** - Daily/monthly sales and expense reports
- **Inventory Management** - Add/edit shop items with pricing hints

### 🔮 Planned Features
- **Cloud Sync** - Firebase integration for data backup
- **Receipt Generation** - Print/share sale receipts
- **Multi-currency Support** - Support for different currencies
- **Advanced Reports** - Detailed analytics and insights

## Technical Stack

- **Language**: Kotlin
- **Architecture**: MVVM with Repository pattern
- **UI**: Material Design 3, ViewBinding
- **Database**: Room (SQLite)
- **Navigation**: Navigation Component
- **Async**: Coroutines + Flow
- **DI**: Manual dependency injection (planned: Hilt/Dagger)
- **Cloud**: Firebase (Firestore, Auth)

## Database Schema

### Entities
- **User** - User accounts with roles (owner, cashier)
- **Item** - Shop items with categories (Product/Service) and pricing hints
- **Sale** - Sales transactions with itemized details
- **Expense** - Expense records with categories

### Key Features
- **Free Value Pricing** - Users can enter any price for items
- **Per-Kg Hints** - Products can have price-per-kg suggestions
- **Category System** - Products vs Services with different pricing models
- **Multi-user Support** - Track sales/expenses by user

## Getting Started

### Prerequisites
- Android Studio Arctic Fox or later
- Android SDK 24+ (Android 7.0)
- Kotlin 1.9+

### Setup
1. Clone the repository
2. Open in Android Studio
3. Update `google-services.json` with your Firebase config
4. Build and run

### Default Login
- Username: `admin`
- Password: Any password (demo mode)
- Or tap "Create Demo User" to set up a test account

## Project Structure

```
app/src/main/java/com/shopkeeper/pro/
├── data/
│   ├── database/        # Room database setup
│   ├── dao/            # Data Access Objects
│   ├── entity/         # Database entities
│   └── repository/     # Data repositories
├── ui/
│   ├── auth/          # Login/authentication
│   ├── dashboard/     # Main dashboard
│   ├── sales/         # Sales calculator (planned)
│   ├── expenses/      # Expense tracker (planned)
│   └── reports/       # Reports & analytics (planned)
└── MainActivity.kt    # Main navigation host
```

## Expense Categories
- Purchase
- Daily Wages  
- Bills
- Rent
- Electricity
- Transport
- Other

## Default Shop Items
- **Podipp** (Product, ₹50/kg)
- **Chilly** (Product, ₹80/kg)
- **Malli** (Product, ₹120/kg)
- **Unda** (Product, per piece)
- **Repair Service** (Service)

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Roadmap

### Phase 1 (Current)
- ✅ Basic project structure
- ✅ User authentication
- ✅ Dashboard overview
- 🚧 Sales calculator implementation

### Phase 2
- Expense tracking
- Item management
- Basic reporting

### Phase 3
- Firebase integration
- Advanced reports
- Receipt generation
- Multiple shop support

## Support

For issues and feature requests, please create an issue in the GitHub repository.