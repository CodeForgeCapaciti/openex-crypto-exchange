// src/components/Charts/PriceChart.js
import React from 'react';
import { LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer } from 'recharts';
import { Box, Typography } from '@mui/material';

function PriceChart({ trades, ticker }) {
  // Prepare chart data from trades
  const chartData = (trades || []).slice().reverse().map((trade, index) => ({
    time: new Date(trade.timestamp).toLocaleTimeString(),
    price: parseFloat(trade.price),
    quantity: parseFloat(trade.quantity)
  }));

  const currentPrice = ticker?.price || 50000;
  const change24h = ticker?.change24h || 0;
  const isPositive = change24h >= 0;

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">BTC/USD Price Chart</Typography>
        <Box>
          <Typography variant="h5" component="span" sx={{ mr: 2 }}>
            ${currentPrice.toFixed(2)}
          </Typography>
          <Typography
            variant="body1"
            component="span"
            sx={{ color: isPositive ? 'success.main' : 'error.main' }}
          >
            {isPositive ? '+' : ''}{change24h.toFixed(2)}%
          </Typography>
        </Box>
      </Box>

      <ResponsiveContainer width="100%" height={320}>
        <LineChart data={chartData}>
          <CartesianGrid strokeDasharray="3 3" stroke="#333" />
          <XAxis dataKey="time" stroke="#888" />
          <YAxis stroke="#888" domain={['auto', 'auto']} />
          <Tooltip
            contentStyle={{ backgroundColor: '#1a1a1a', border: '1px solid #333' }}
            labelStyle={{ color: '#fff' }}
          />
          <Line
            type="monotone"
            dataKey="price"
            stroke="#ff9800"
            strokeWidth={2}
            dot={false}
          />
        </LineChart>
      </ResponsiveContainer>

      {chartData.length === 0 && (
        <Typography color="text.secondary" align="center" sx={{ mt: 4 }}>
          Waiting for market data...
        </Typography>
      )}
    </Box>
  );
}

export default PriceChart;