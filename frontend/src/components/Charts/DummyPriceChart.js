// src/components/Charts/DummyPriceChart.js
import React, { useState, useEffect } from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Box, Typography, Paper, Grid } from '@mui/material';

function DummyPriceChart() {
  const [chartData, setChartData] = useState([]);
  const [currentPrice, setCurrentPrice] = useState(50000);
  const [priceChange, setPriceChange] = useState(0);
  const [priceChangePercent, setPriceChangePercent] = useState(0);
  const [high24h, setHigh24h] = useState(50200);
  const [low24h, setLow24h] = useState(49800);
  const [volume24h, setVolume24h] = useState(1250.5);

  // Generate dummy data on mount
  useEffect(() => {
    generateDummyData();

    // Update price every 3 seconds for dynamic feel
    const interval = setInterval(() => {
      updatePrice();
    }, 3000);

    return () => clearInterval(interval);
  }, []);

  const generateDummyData = () => {
    const data = [];
    const now = new Date();

    // Generate 50 data points for the last hour
    for (let i = 50; i >= 0; i--) {
      const time = new Date(now.getTime() - i * 120000); // Every 2 minutes
      // Small random variation around 50000
      const randomVariation = (Math.random() - 0.5) * 200;
      const price = 50000 + randomVariation;

      data.push({
        time: time.toLocaleTimeString(),
        price: price,
        volume: Math.random() * 10
      });
    }

    setChartData(data);
    setCurrentPrice(data[data.length - 1]?.price || 50000);
    calculatePriceChange(data);
  };

  const updatePrice = () => {
    setChartData(prevData => {
      if (prevData.length === 0) return prevData;

      // Small random price movement (±50)
      const change = (Math.random() - 0.5) * 100;
      const newPrice = Math.max(49000, Math.min(51000, currentPrice + change));

      const newTime = new Date();
      const newDataPoint = {
        time: newTime.toLocaleTimeString(),
        price: newPrice,
        volume: Math.random() * 10
      };

      // Add new point and remove oldest to keep 50 points
      const updatedData = [...prevData.slice(1), newDataPoint];
      setCurrentPrice(newPrice);
      calculatePriceChange(updatedData);

      // Update 24h stats
      update24hStats(newPrice);

      return updatedData;
    });
  };

  const calculatePriceChange = (data) => {
    if (data.length < 2) return;

    const firstPrice = data[0]?.price || 50000;
    const lastPrice = data[data.length - 1]?.price || 50000;
    const change = lastPrice - firstPrice;
    const changePercent = (change / firstPrice) * 100;

    setPriceChange(change);
    setPriceChangePercent(changePercent);
  };

  const update24hStats = (newPrice) => {
    setHigh24h(prev => Math.max(prev, newPrice));
    setLow24h(prev => Math.min(prev, newPrice));
    setVolume24h(prev => prev + (Math.random() * 5));
  };

  const formatNumber = (num) => {
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(num);
  };

  const formatVolume = (num) => {
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 2
    }).format(num);
  };

  const isPositive = priceChange >= 0;

  return (
    <Box>
      {/* Header with current price */}
      <Box sx={{ mb: 3 }}>
        <Typography variant="h4" component="span" sx={{ fontWeight: 'bold', mr: 2 }}>
          ${formatNumber(currentPrice)}
        </Typography>
        <Typography
          variant="h6"
          component="span"
          sx={{
            color: isPositive ? 'success.main' : 'error.main',
            fontWeight: 'bold'
          }}
        >
          {isPositive ? '+' : ''}{priceChangePercent.toFixed(2)}%
        </Typography>
        <Typography variant="body2" color="text.secondary" sx={{ mt: 1 }}>
          BTC/USD • Real-time
        </Typography>
      </Box>

      {/* Stats Grid */}
      <Grid container spacing={2} sx={{ mb: 3 }}>
        <Grid item xs={6} sm={3}>
          <Paper sx={{ p: 1.5, bgcolor: 'background.paper' }}>
            <Typography variant="caption" color="text.secondary">
              24h Change
            </Typography>
            <Typography
              variant="body1"
              sx={{
                color: isPositive ? 'success.main' : 'error.main',
                fontWeight: 'bold'
              }}
            >
              {isPositive ? '+' : ''}{priceChangePercent.toFixed(2)}%
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={6} sm={3}>
          <Paper sx={{ p: 1.5, bgcolor: 'background.paper' }}>
            <Typography variant="caption" color="text.secondary">
              24h High
            </Typography>
            <Typography variant="body1" fontWeight="bold">
              ${formatNumber(high24h)}
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={6} sm={3}>
          <Paper sx={{ p: 1.5, bgcolor: 'background.paper' }}>
            <Typography variant="caption" color="text.secondary">
              24h Low
            </Typography>
            <Typography variant="body1" fontWeight="bold">
              ${formatNumber(low24h)}
            </Typography>
          </Paper>
        </Grid>
        <Grid item xs={6} sm={3}>
          <Paper sx={{ p: 1.5, bgcolor: 'background.paper' }}>
            <Typography variant="caption" color="text.secondary">
              24h Volume (BTC)
            </Typography>
            <Typography variant="body1" fontWeight="bold">
              {formatVolume(volume24h)}
            </Typography>
          </Paper>
        </Grid>
      </Grid>

      {/* Chart */}
      <Box sx={{ height: 350, width: '100%' }}>
        <ResponsiveContainer>
          <LineChart data={chartData}>
            <CartesianGrid strokeDasharray="3 3" stroke="#333" />
            <XAxis
              dataKey="time"
              stroke="#888"
              tick={{ fill: '#888', fontSize: 12 }}
              interval={Math.floor(chartData.length / 6)}
            />
            <YAxis
              stroke="#888"
              tick={{ fill: '#888', fontSize: 12 }}
              domain={['auto', 'auto']}
              tickFormatter={(value) => `$${formatNumber(value)}`}
            />
            <Tooltip
              contentStyle={{
                backgroundColor: '#1a1a1a',
                border: '1px solid #333',
                borderRadius: '4px'
              }}
              labelStyle={{ color: '#fff' }}
              formatter={(value) => [`$${formatNumber(value)}`, 'Price']}
            />
            <Line
              type="monotone"
              dataKey="price"
              stroke="#ff9800"
              strokeWidth={2}
              dot={false}
              activeDot={{ r: 6, fill: '#ff9800' }}
              isAnimationActive={true}
            />
          </LineChart>
        </ResponsiveContainer>
      </Box>

      {/* Additional Info */}
      <Box sx={{ mt: 2, display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
        <Typography variant="caption" color="text.secondary">
          Last updated: {new Date().toLocaleTimeString()}
        </Typography>
        <Typography variant="caption" color="text.secondary">
          Data is simulated for demo purposes
        </Typography>
      </Box>
    </Box>
  );
}

export default DummyPriceChart;