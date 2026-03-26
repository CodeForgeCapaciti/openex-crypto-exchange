// src/components/TradeHistory/index.js
import React from 'react';
import { Table, TableBody, TableCell, TableContainer, TableHead, TableRow, Typography, Box } from '@mui/material';

function TradeHistory({ trades }) {
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
        Recent Trades
      </Typography>

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Time</TableCell>
              <TableCell align="right">Price (USD)</TableCell>
              <TableCell align="right">Amount (BTC)</TableCell>
              <TableCell align="right">Total (USD)</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {trades && trades.slice(0, 20).map((trade, index) => (
              <TableRow key={index}>
                <TableCell>
                  {new Date(trade.timestamp).toLocaleTimeString()}
                </TableCell>
                <TableCell align="right">
                  <Typography
                    variant="body2"
                    sx={{ color: trade.side === 'buy' ? 'success.main' : 'error.main' }}
                  >
                    ${formatNumber(trade.price)}
                  </Typography>
                </TableCell>
                <TableCell align="right">
                  {formatNumber(trade.quantity)}
                </TableCell>
                <TableCell align="right">
                  ${formatNumber(trade.price * trade.quantity)}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {(!trades || trades.length === 0) && (
        <Typography color="text.secondary" align="center" sx={{ py: 4 }}>
          No recent trades
        </Typography>
      )}
    </Box>
  );
}

export default TradeHistory;