import { PaymentStatusEnum } from '../types';

const BASE_URL = 'http://localhost:9090';

export const usePaymentApi = () => {
  const fetchConfig = async () => {
    try {
      const response = await fetch(`${BASE_URL}/api/config`);
      if (response.ok) {
        const result = await response.json(); console.log("[usePaymentApi] Config fetched:", result); return result;
      }
    } catch (e) {
      console.warn('Could not fetch config endpoint:', e);
    }
    return null;
  };

  const createOrder = async (amountInPaise: number, bookingId: string) => {
    const response = await fetch(`${BASE_URL}/api/create-order`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify({
        amount: amountInPaise,
        currency: 'INR',
        receipt: bookingId
      })
    });

    if (!response.ok) {
      const errorData = await response.json().catch(() => ({ message: 'Failed to create order' }));
      throw new Error(errorData.message || 'Error creating Razorpay order');
    }
    const result = await response.json(); console.log("[usePaymentApi] Order created:", result); return result;
  };

  const logPayment = async (payload: {
    bookingId: string;
    amount: number;
    currency: string;
    status: PaymentStatusEnum;
    failureReason: string | null;
    timestamps: string;
    razorpayOrderId?: string | null;
    razorpayPaymentId?: string | null;
    razorpaySignature?: string | null;
  }, idempotencyKey: string) => {
    try {
      const response = await fetch(`${BASE_URL}/api/v1/payment`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', 'x-idempotency-key': idempotencyKey },
        body: JSON.stringify(payload)
      });
      if (!response.ok) {
        const errorText = await response.text();
        console.error('[usePaymentApi] Payment log failed:', response.status, errorText);
        throw new Error(`Payment log failed: ${response.status} ${errorText}`);
      }
      return await response.json();
    } catch (e) {
      console.warn('Backend payment log failed:', e);
      throw e;
    }
  };

  const getPaymentByBookingId = async (bookingId: string) => {
    try {
      const response = await fetch(`${BASE_URL}/api/v1/payments/by-booking/${bookingId}`);
      if (response.ok) {
        return await response.json();
      }
      if (response.status === 404) return null;
      throw new Error(`Failed to fetch payment status: ${response.status}`);
    } catch (e) {
      console.warn('[usePaymentApi] Error fetching payment by bookingId:', e);
      return null;
    }
  };

  const verifyPayment = async (verificationData: {
    razorpay_order_id: string;
    razorpay_payment_id: string;
    razorpay_signature: string;
  }) => {
    console.log("[usePaymentApi] Sending verification data to backend:", verificationData);
    const response = await fetch(`${BASE_URL}/api/verify-payment`, {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json'
      },
      body: JSON.stringify(verificationData)
    });

    if (!response.ok) {
      const errData = await response.json().catch(() => ({ message: 'Verification failed' }));
      throw new Error(errData.message || 'Verification failed');
    }
    const result = await response.json();
    console.log("[usePaymentApi] Received verification response:", result);
    return result;
  };

  return {
    fetchConfig,
    createOrder,
    logPayment,
    getPaymentByBookingId,
    verifyPayment
  };
};
