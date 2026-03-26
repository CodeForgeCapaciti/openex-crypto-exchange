// src/components/TradingForm/index.js
import React, { useState } from 'react';
import { Box, TextField, Button, ToggleButton, ToggleButtonGroup, Typography, Alert } from '@mui/material';
import toast from 'react-hot-toast';

function TradingForm({ onSubmit, currentPrice }) {
  const [side, setSide] = useState('BUY');
  const [orderType, setOrderType] = useState('LIMIT');
  const [price, setPrice] = useState('');
  const [quantity, setQuantity] = useState('');
  const [error, setError] = useState('');

  const handleSideChange = (event, newSide) => {
    if (newSide !== null) setSide(newSide);
  };

  const handleTypeChange = (event, newType) => {
    if (newType !== null) setOrderType(newType);
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    setError('');

    if (!quantity || parseFloat(quantity) <= 0) {
      setError('Please enter a valid quantity');
      return;
    }

    if (orderType === 'LIMIT' && (!price || parseFloat(price) <= 0)) {
      setError('Please enter a valid price');
      return;
    }

    try {
      await onSubmit({
        symbol: 'BTC/USD',
        side,
        type: orderType,
        price: orderType === 'LIMIT' ? parseFloat(price) : (currentPrice || 50000),
        quantity: parseFloat(quantity)
      });

      toast.success(`${side} order placed successfully!`);
      setQuantity('');
      if (orderType === 'LIMIT') setPrice('');
    } catch (err) {
      setError(err.message || 'Failed to place order');
      toast.error('Failed to place order');
    }
  };

  return (
    <Box component="form" onSubmit={handleSubmit}>
      <Typography variant="h6" gutterBottom>
        Place Order
      </Typography>

      <ToggleButtonGroup
        value={side}
        exclusive
        onChange={handleSideChange}
        sx={{ mb: 2, width: '100%' }}
      >
        <ToggleButton value="BUY" sx={{ flex: 1, bgcolor: side === 'BUY' ? 'success.dark' : 'inherit' }}>
          BUY BTC
        </ToggleButton>
        <ToggleButton value="SELL" sx={{ flex: 1, bgcolor: side === 'SELL' ? 'error.dark' : 'inherit' }}>
          SELL BTC
        </ToggleButton>
      </ToggleButtonGroup>

      <ToggleButtonGroup
        value={orderType}
        exclusive
        onChange={handleTypeChange}
        sx={{ mb: 2, width: '100%' }}
      >
        <ToggleButton value="LIMIT" sx={{ flex: 1 }}>
          LIMIT
        </ToggleButton>
        <ToggleButton value="MARKET" sx={{ flex: 1 }}>
          MARKET
        </ToggleButton>
      </ToggleButtonGroup>

      {orderType === 'LIMIT' && (
        <TextField
          fullWidth
          label="Price (USD)"
          type="number"
          value={price}
          onChange={(e) => setPrice(e.target.value)}
          sx={{ mb: 2 }}
          InputProps={{ step: '0.01' }}
          placeholder={`Current: $${currentPrice?.toFixed(2) || '--'}`}
        />
      )}

      <TextField
        fullWidth
        label="Quantity (BTC)"
        type="number"
        value={quantity}
        onChange={(e) => setQuantity(e.target.value)}
        sx={{ mb: 2 }}
        InputProps={{ step: '0.0001' }}
      />

      {orderType === 'LIMIT' && price && quantity && (
        <Typography variant="body2" color="text.secondary" sx={{ mb: 2 }}>
          Total: ${(parseFloat(price) * parseFloat(quantity)).toFixed(2)} USD
        </Typography>
      )}

      {error && (
        <Alert severity="error" sx={{ mb: 2 }}>
          {error}
        </Alert>
      )}

      <Button
        type="submit"
        fullWidth
        variant="contained"
        color={side === 'BUY' ? 'success' : 'error'}
        size="large"
      >
        {side} BTC
      </Button>
    </Box>
  );
}

export default TradingForm;