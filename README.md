# OpenEx - Simulated Crypto Exchange

A lightweight simulated crypto exchange supporting BTC/USD trading with limit & market orders, user wallets, order book, matching engine, and real-time WebSocket streaming.

## Features

- ✅ **Trading**: Limit and market orders for BTC/USD
- ✅ **Real-time Data**: WebSocket streaming for live market updates
- ✅ **Order Book**: Visual representation of buy and sell orders
- ✅ **Matching Engine**: Automated order matching with price-time priority
- ✅ **User Wallets**: Manage BTC and USD balances
- ✅ **Dashboard**: React-based trading interface with charts
- ✅ **Authentication**: JWT-based secure authentication
- ✅ **Monitoring**: Prometheus + Grafana for metrics
- ✅ **Containerized**: Docker and Docker Compose for easy deployment
- ✅ **CI/CD**: GitHub Actions for automated testing and deployment

## Tech Stack

### Backend
- Java 17
- Spring Boot 3.x (Web, Data JPA, WebSocket, Security)
- PostgreSQL for persistence
- Redis for caching and real-time data
- JWT for authentication
- Maven for build management

### Frontend
- React 18
- Material-UI for components
- Recharts for data visualization
- WebSocket (STOMP) for real-time data
- Axios for API calls

### DevOps
- Docker & Docker Compose
- Prometheus & Grafana
- GitHub Actions CI/CD

## Prerequisites

- Docker and Docker Compose (for containerized deployment)
- Java 17+ (for local backend development)
- Node.js 18+ (for local frontend development)
- PostgreSQL 15+ (optional, for local development)
- Redis 7+ (optional, for local development)

## Quick Start with Docker

1. Clone the repository:
```bash
git clone https://github.com/yourusername/openex.git
cd openex