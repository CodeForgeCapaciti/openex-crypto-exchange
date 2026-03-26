// src/components/OrderBook/index.js
import React from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, Box } from '@mui/material';

function OrderBook({ bids, asks }) {
  const formatNumber = (num) => {
    if (!num) return '0.00';
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 8
    }).format(num);
  };

  return (
    <Box>
      <Typography variant="h6" gutterBottom>
        Order Book
      </Typography>

      <Typography variant="subtitle2" color="error" gutterBottom>
        Sell Orders
      </Typography>
      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Price (USD)</TableCell>
              <TableCell align="right">Amount (BTC)</TableCell>
              <TableCell align="right">Total (USD)</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {asks && asks.slice(0, 10).map((ask, index) => (
              <TableRow key={index} sx={{ '& td': { color: '#ff5252' } }}>
                <TableCell>{formatNumber(ask[0])}</TableCell>
                <TableCell align="right">{formatNumber(ask[1])}</TableCell>
                <TableCell align="right">{formatNumber(ask[0] * ask[1])}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      <Box sx={{ my: 2, textAlign: 'center', py: 1, bgcolor: 'action.hover' }}>
        <Typography variant="h6" color="primary">
          {bids && bids[0] ? `$${formatNumber(bids[0][0])}` : '$--'}
        </Typography>
      </Box>

      <Typography variant="subtitle2" color="success.main" gutterBottom>
        Buy Orders
      </Typography>
      <TableContainer>
        <Table size="small">
          <TableBody>
            {bids && bids.slice(0, 10).map((bid, index) => (
              <TableRow key={index} sx={{ '& td': { color: '#4caf50' } }}>
                <TableCell>{formatNumber(bid[0])}</TableCell>
                <TableCell align="right">{formatNumber(bid[1])}</TableCell>
                <TableCell align="right">{formatNumber(bid[0] * bid[1])}</TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>
    </Box>
  );
}

export default OrderBook;