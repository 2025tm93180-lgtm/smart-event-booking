import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useSelector } from 'react-redux';
import { RootState } from '../store';
import { money } from '../utils';
import {Booking, PaymentStatusEnum, User} from '../types';
import { usePaymentApi } from '../hooks/usePaymentApi';
import { useBookingApi } from '../hooks/useBookingApi';
import { Zap, ShieldCheck, RefreshCw } from 'lucide-react';

interface PaymentProps {
  booking: Booking;
  user: User | null;
  onSuccess: (paymentId: string, newBookingId?: string) => void;
  onCancel: () => void;
  onError: (message: string) => void;
}

export const Payment: React.FC<PaymentProps> = ({ booking, user: propUser, onSuccess, onCancel, onError }) => {
  const navigate = useNavigate();
  const { user: storeUser } = useSelector((state: RootState) => state.user);
  const currentUser = storeUser || propUser;

  const [loading, setLoading] = useState(false);
  const [status, setStatus] = useState<{ message: string; type: 'success' | 'error' | 'info' | null }>({
    message: '',
    type: null
  });

  const { fetchConfig, createOrder, logPayment, getPaymentByBookingId, verifyPayment } = usePaymentApi();
  const { createBooking } = useBookingApi();
  const json = {
    bookingId: "AF-07",
    "status": "PENDING",
    "paymentId": "PAY_ID_1234567890",
    "amount": 100,
    "currency": "INR",
    "timestamp": "2023-08-15T12:34:56Z",
    "failureReason": null,
    "razorpayOrderId": "ORDER_ID_1234567890",
    "razorpayPaymentId": "PAY_ID_1234567890",
    "razorpaySignature": "SIGNATURE_1234567890"
  };

  const showStatus = (message: string, type: 'success' | 'error' | 'info') => {
    setStatus({ message, type });
  };

  const initiatePayment = async () => {
    if (!currentUser) {
      showStatus('Please sign up or log in to continue with payment.', 'error');
      setTimeout(() => {
        navigate('/signup');
      }, 2000);
      return;
    }

    const amountInPaise = Math.round(booking.amount * 100);

    try {
      setLoading(true);
      let storedBookingId = sessionStorage.getItem('bookingId');
      let realBookingId = '';

      if (storedBookingId) {
        // call booking api to know the latest status of payment
        try {
          const bookingResp = json;//await fetch(`http://localhost:9091/api/v1/bookings/${storedBookingId}`);

          //if (bookingResp.ok) {
          if (json) {
            const bookingDataJson = json;//await bookingResp.json();

            
            // If already confirmed, move forward
            if (bookingDataJson.status === 'CONFIRMED') {
              showStatus('Booking already confirmed!', 'success');
              onSuccess(bookingDataJson.paymentId || 'PREVIOUS_PAYMENT', storedBookingId);
              sessionStorage.removeItem('bookingId');
              return;
            }
            
            // If pending, check payment service for existing order
            if (bookingDataJson.status === 'PENDING') {
              realBookingId = storedBookingId;
              const existingPayment = await getPaymentByBookingId(realBookingId);
              //const existingPayment = json;
              
              if (existingPayment) {
                //already payment done
                if (existingPayment.status === 'CONFIRMED') {
                  showStatus('Payment already verified. Updating booking...', 'success');
                  onSuccess(existingPayment.razorpayPaymentId, realBookingId);
                  return;
                }
                //existing ID found but payment not happen
                if (existingPayment.razorpayOrderId && existingPayment.status === 'PENDING') {
                  showStatus('Resuming existing payment session...', 'info');
                  // We have an existing order, we can reuse it
                  await openRazorpayCheckout({
                    order_id: existingPayment.razorpayOrderId,
                    amount: amountInPaise,
                    currency: existingPayment.currency || 'INR'
                  }, realBookingId);
                  return;
                }
              }
            }
          }
        } catch (fetchErr) {
          console.warn('Failed to fetch existing booking status:', fetchErr);
        }
      }

      if (!realBookingId) {
        showStatus('Creating booking...', 'info');
         const bookingData = json;
        //      await createBooking({
        //   seatNumbers: booking.seats,
        //   venue: booking.event.venue,
        //   username: currentUser?.name ?? 'Guest User',
        //   mail: currentUser?.email ?? '',
        //   phoneNumber: currentUser?.phone ?? '',
        //   eventId: booking.event.id,
        //   eventTitle: booking.event.title,
        //   eventDate: booking.event.date,
        //   eventStartTime: booking.event.startTime,
        //   eventDuration: booking.event.duration,
        //   city: booking.event.city,
        //   amount: booking.amount,
        //   currency: 'INR',
        //   status: 'PENDING',
        // });


        if (!bookingData || !bookingData.bookingId) {
          throw new Error('Failed to create booking on server');
        }
        realBookingId = bookingData.bookingId;
      }

      showStatus('Preparing payment order...', 'info');
      const orderData = await createOrder(amountInPaise, realBookingId);
      
      // Log the initial pending payment attempt
      await logPayment({
        bookingId: realBookingId,
        amount: booking.amount,
        currency: 'INR',
        status: 'PENDING' as PaymentStatusEnum,
        razorpayOrderId: orderData.order_id,
        failureReason: null,
        timestamps: new Date().toISOString()
      }, `order_${realBookingId}_${orderData.order_id}`);

      await openRazorpayCheckout(orderData, realBookingId);

    } catch (err: any) {
      showStatus('Error: ' + err.message, 'error');
      setLoading(false);
      onError(err.message);
    }
  };

  const openRazorpayCheckout = async (orderData: any, realBookingId: string) => {
    let razorpayKeyId = 'rzp_test_TmUoRlZbl3o0Vo'; // Fallback
    try {
      const configData = await fetchConfig();
      if (configData?.keyId) razorpayKeyId = configData.keyId;
    } catch (e) { console.warn('Config fetch failed', e); }

    // @ts-ignore
    if (typeof window.Razorpay === 'undefined') {
      throw new Error('Razorpay SDK not loaded. Please check your internet connection.');
    }

    // @ts-ignore
    const options = {
      key: razorpayKeyId,
      amount: orderData.amount,
      currency: orderData.currency || 'INR',
      name: 'Smart Event Tickets',
      description: 'Payment for Booking #' + realBookingId,
      order_id: orderData.order_id,
      handler: async function (response: any) {
        showStatus('Verifying payment...', 'info');
        try {
          // 1. Log success attempt
          await logPayment({
            bookingId: realBookingId,
            amount: booking.amount,
            currency: orderData.currency || 'INR',
            status: 'CONFIRMED' as PaymentStatusEnum,
            razorpayOrderId: response.razorpay_order_id,
            razorpayPaymentId: response.razorpay_payment_id,
            razorpaySignature: response.razorpay_signature,
            failureReason: null,
            timestamps: new Date().toISOString()
          }, `order_${realBookingId}_${orderData.order_id}`);

          // 2. Verify signature
          await verifyPayment({
            razorpay_order_id: response.razorpay_order_id,
            razorpay_payment_id: response.razorpay_payment_id,
            razorpay_signature: response.razorpay_signature
          });

          showStatus('Payment successful!', 'success');
          onSuccess(response.razorpay_payment_id, realBookingId);
        } catch (verifyErr: any) {
          showStatus('Verification failed: ' + verifyErr.message, 'error');
          onError(verifyErr.message);
        } finally {
          setLoading(false);
        }
      },
      modal: {
        ondismiss: async function () {
          showStatus('Payment cancelled.', 'info');
          setLoading(false);
          onCancel();
        }
      },
      theme: { color: '#3399cc' }
    };

    // @ts-ignore
    const rzp = new window.Razorpay(options);
    rzp.on('payment.failed', async function (response: any) {
      const reason = response.error.description || 'Transaction failed';
      showStatus('Payment failed: ' + reason, 'error');
      
      try {
        await logPayment({
          bookingId: realBookingId,
          amount: booking.amount,
          currency: 'INR',
          status: 'FAILED' as PaymentStatusEnum,
          razorpayOrderId: response.error.metadata?.order_id,
          razorpayPaymentId: response.error.metadata?.payment_id,
          failureReason: reason,
          timestamps: new Date().toISOString()
        }, `order_${realBookingId}_${orderData.order_id}`);
      } catch (e) { console.error('Failed to log failure', e); }

      setLoading(false);
      onError(reason);
    });
    rzp.open();
  };
  const seatsToDisplay = booking.seats.join(', ');

  return (
    <div className="payment-card">
      <h3>Complete your Payment</h3>
      
      <div className="payment-summary">
        <div className="payment-row">
          <span>Amount to Pay</span>
          <strong>{money(booking.amount)}</strong>
        </div>
        <div className="payment-row">
          <span>Customer Name</span>
          <code>{currentUser?.name || 'Guest'}</code>
        </div>
        <div className="payment-row">
          <span>Email Address</span>
          <code>{currentUser?.email || 'N/A'}</code>
        </div>
        <div className="payment-row">
          <span>Seats</span>
          <code>{seatsToDisplay}</code>
        </div>
      </div>

      <button 
        id="pay-btn" 
        className="button button-primary full-button" 
        onClick={initiatePayment} 
        disabled={loading}
      >
        {loading ? (
          <span className="flex items-center justify-center gap-2">
            <RefreshCw size={16} className="animate-spin" />
            Processing...
          </span>
        ) : 'Pay Now with Razorpay'}
      </button>

      {status.type && (
        <div className={`status-box ${status.type}`}>
          <div className="flex items-center gap-2">
            {status.type === 'info' && <RefreshCw size={14} className="animate-spin" />}
            {status.message}
          </div>
        </div>
      )}
      
      <p className="fine-print centered">
        Secure payment powered by Razorpay.
      </p>
    </div>
  );
};
