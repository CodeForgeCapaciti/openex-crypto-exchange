// src/components/Wallet/index.js
import React, { useState } from 'react';
import {
  Table, TableBody, TableCell, TableContainer,
  TableHead, TableRow, Typography, Box, Button,
  Dialog, DialogTitle, DialogContent, TextField,
  DialogActions, Alert
} from '@mui/material';
import api from '../../services/api';
import toast from 'react-hot-toast';

function Wallet({ wallets }) {
  const [openDialog, setOpenDialog] = useState(false);
  const [newCurrency, setNewCurrency] = useState('');
  const [newBalance, setNewBalance] = useState('');
  const [loading, setLoading] = useState(false);

  const handleCreateWallet = async () => {
    if (!newCurrency) {
      toast.error('Please enter a currency');
      return;
    }

    const balance = parseFloat(newBalance) || 0;

    setLoading(true);
    try {
      const response = await api.createWallet(newCurrency.toUpperCase(), balance);
      toast.success(`Wallet created for ${newCurrency.toUpperCase()}`);
      setOpenDialog(false);
      setNewCurrency('');
      setNewBalance('');
      setTimeout(() => window.location.reload(), 1000);
    } catch (error) {
      const errorMsg = error.response?.data?.error || 'Failed to create wallet';
      toast.error(errorMsg);
    } finally {
      setLoading(false);
    }
  };

  const handleDeposit = async (currency) => {
    const amount = prompt(`Enter amount to deposit in ${currency}:`);
    if (amount && !isNaN(amount) && parseFloat(amount) > 0) {
      try {
        await api.deposit(currency, amount);
        toast.success(`Deposited ${amount} ${currency}`);
        setTimeout(() => window.location.reload(), 1000);
      } catch (error) {
        toast.error(error.response?.data?.error || 'Failed to deposit');
      }
    }
  };

  const formatNumber = (num) => {
    if (!num) return '0.00';
    return new Intl.NumberFormat('en-US', {
      minimumFractionDigits: 2,
      maximumFractionDigits: 8
    }).format(num);
  };

  return (
    <Box>
      <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 2 }}>
        <Typography variant="h6">
          Wallet Balance
        </Typography>
        <Button
          variant="outlined"
          size="small"
          onClick={() => setOpenDialog(true)}
        >
          + New Wallet
        </Button>
      </Box>

      <TableContainer>
        <Table size="small">
          <TableHead>
            <TableRow>
              <TableCell>Currency</TableCell>
              <TableCell align="right">Total Balance</TableCell>
              <TableCell align="right">Available</TableCell>
              <TableCell align="right">Frozen</TableCell>
              <TableCell align="right">Action</TableCell>
            </TableRow>
          </TableHead>
          <TableBody>
            {wallets && wallets.map((wallet) => (
              <TableRow key={wallet.currency}>
                <TableCell>
                  <Typography variant="body1" fontWeight="bold">
                    {wallet.currency}
                  </Typography>
                </TableCell>
                <TableCell align="right">
                  <Typography variant="body1">
                    {formatNumber(wallet.balance)}
                  </Typography>
                </TableCell>
                <TableCell align="right">
                  <Typography variant="body2" color="text.secondary">
                    {formatNumber(wallet.availableBalance)}
                  </Typography>
                </TableCell>
                <TableCell align="right">
                  <Typography variant="body2" color="text.secondary">
                    {formatNumber(wallet.frozenBalance)}
                  </Typography>
                </TableCell>
                <TableCell align="right">
                  {wallet.currency === 'USD' && (
                    <Button size="small" onClick={() => handleDeposit('USD')}>
                      Deposit
                    </Button>
                  )}
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      </TableContainer>

      {(!wallets || wallets.length === 0) && (
        <Typography color="text.secondary" align="center" sx={{ py: 4 }}>
          No wallets yet. Click "New Wallet" to create one.
        </Typography>
      )}

      {/* Create Wallet Dialog */}
      <Dialog open={openDialog} onClose={() => setOpenDialog(false)}>
        <DialogTitle>Create New Wallet</DialogTitle>
        <DialogContent>
          <TextField
            autoFocus
            margin="dense"
            label="Currency (e.g., ETH, SOL, XRP)"
            fullWidth
            variant="outlined"
            value={newCurrency}
            onChange={(e) => setNewCurrency(e.target.value.toUpperCase())}
            placeholder="ETH"
          />
          <TextField
            margin="dense"
            label="Initial Balance"
            type="number"
            fullWidth
            variant="outlined"
            value={newBalance}
            onChange={(e) => setNewBalance(e.target.value)}
            placeholder="0"
          />
          <Alert severity="info" sx={{ mt: 2 }}>
            You can create wallets for any cryptocurrency. The initial balance can be set to any amount.
          </Alert>
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setOpenDialog(false)}>Cancel</Button>
          <Button onClick={handleCreateWallet} variant="contained" disabled={loading}>
            {loading ? 'Creating...' : 'Create Wallet'}
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  );
}

export default Wallet;