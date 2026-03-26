// src/components/Dashboard/index.js
import React, { useState, useEffect } from 'react';
import { Grid, Paper, Typography, Box, AppBar, Toolbar, IconButton, CircularProgress, Button } from '@mui/material';
import { useAuth } from '../../contexts/AuthContext';
import OrderBook from '../OrderBook';
import TradingForm from '../TradingForm';
import Wallet from '../Wallet';
import DummyPriceChart from '../Charts/DummyPriceChart';
import TradeHistory from '../TradeHistory';
import { websocketService } from '../../services/websocket';
import api from '../../services/api';
import LogoutIcon from '@mui/icons-material/Logout';

function Dashboard() {
  const { user, logout } = useAuth();
  const [orderBook, setOrderBook] = useState({ bids: [], asks: [] });
  const [trades, setTrades] = useState([]);
  const [userOrders, setUserOrders] = useState([]);
  const [wallets, setWallets] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    // Connect to WebSocket for order book and trade updates
    websocketService.connect();
    websocketService.onMessage((data) => {
      console.log('WebSocket data received:', data.type);
      if (data.type === 'market_data') {
        if (data.data?.orderBook) setOrderBook(data.data.orderBook);
        if (data.data?.trades) setTrades(data.data.trades);
      }
    });

    // Fetch initial data
    fetchUserData();

    // Refresh data every 5 seconds
    const interval = setInterval(fetchUserData, 5000);

    return () => {
      websocketService.disconnect();
      clearInterval(interval);
    };
  }, []);

  const fetchUserData = async () => {
    try {
      const [orders, walletsData] = await Promise.all([
        api.getOrders(),
        api.getWallets()
      ]);
      setUserOrders(orders);
      setWallets(walletsData);
    } catch (error) {
      console.error('Error fetching user data:', error);
    } finally {
      setLoading(false);
    }
  };

  const handlePlaceOrder = async (order) => {
    try {
      await api.placeOrder(order);
      fetchUserData();
    } catch (error) {
      console.error('Error placing order:', error);
      throw error;
    }
  };

  const handleCancelOrder = async (orderId) => {
    try {
      await api.cancelOrder(orderId);
      fetchUserData();
    } catch (error) {
      console.error('Error cancelling order:', error);
    }
  };

  const OrderRow = ({ order, onCancel }) => (
    <Paper sx={{ p: 2, mb: 1, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
      <Box>
        <Typography variant="body2">
          {order.side} {order.type} {order.quantity} BTC @ ${order.price}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Status: {order.status} | Filled: {order.filledQuantity} BTC
        </Typography>
      </Box>
      {(order.status === 'PENDING' || order.status === 'PARTIALLY_FILLED') && (
        <Button size="small" color="error" onClick={() => onCancel(order.id)}>
          Cancel
        </Button>
      )}
    </Paper>
  );

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="100vh">
        <CircularProgress />
      </Box>
    );
  }

  return (
    <Box sx={{ flexGrow: 1 }}>
      <AppBar position="static">
        <Toolbar>
          <Typography variant="h6" sx={{ flexGrow: 1 }}>
            OpenEx - Simulated Crypto Exchange
          </Typography>
          <Typography variant="body1" sx={{ mr: 2 }}>
            Welcome, {user?.username}
          </Typography>
          <IconButton color="inherit" onClick={logout}>
            <LogoutIcon />
          </IconButton>
        </Toolbar>
      </AppBar>

      <Box sx={{ p: 3 }}>
        <Grid container spacing={3}>
          {/* Price Chart - Now using Dummy Chart */}
          <Grid item xs={12} md={8}>
            <Paper sx={{ p: 2 }}>
              <DummyPriceChart />
            </Paper>
          </Grid>

          {/* Order Book */}
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2, height: '500px', overflow: 'auto' }}>
              <OrderBook bids={orderBook.bids} asks={orderBook.asks} />
            </Paper>
          </Grid>

          {/* Trading Form */}
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2 }}>
              <TradingForm onSubmit={handlePlaceOrder} currentPrice={50000} />
            </Paper>
          </Grid>

          {/* Wallet */}
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2 }}>
              <Wallet wallets={wallets} />
            </Paper>
          </Grid>

          {/* Trade History */}
          <Grid item xs={12} md={4}>
            <Paper sx={{ p: 2, height: '300px', overflow: 'auto' }}>
              <TradeHistory trades={trades} />
            </Paper>
          </Grid>

          {/* User Orders */}
          <Grid item xs={12}>
            <Paper sx={{ p: 2 }}>
              <Typography variant="h6" gutterBottom>
                My Orders
              </Typography>
              <Box sx={{ maxHeight: '400px', overflow: 'auto' }}>
                {userOrders.length === 0 ? (
                  <Typography color="text.secondary" align="center" sx={{ py: 4 }}>
                    No orders yet
                  </Typography>
                ) : (
                  userOrders.map(order => (
                    <OrderRow key={order.id} order={order} onCancel={handleCancelOrder} />
                  ))
                )}
              </Box>
            </Paper>
          </Grid>
        </Grid>
      </Box>
    </Box>
  );
}

export default Dashboard;