// Mirrors the backend DTOs. Keep in sync with com.example.eCommerce.*.dtos.

export type AuthProvider = 'LOCAL' | 'GOOGLE';

export interface AuthResponse {
  accessToken: string;
  tokenType: 'Bearer';
  expiresIn: number;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface RegisterRequest {
  name: string;
  email: string;
  password: string;
  dateOfBirth: string; // yyyy-MM-dd
  phoneNumber: string;
  region: string; // ISO 3166-1 alpha-2, e.g. "EG"
}

export interface UpdateProfileRequest {
  name: string;
  dateOfBirth?: string | null;
  phoneNumber?: string | null;
  region?: string | null;
}

export interface ChangePasswordRequest {
  currentPassword?: string | null;
  newPassword: string;
  confirmedNewPassword: string;
}

export interface CustomerProfile {
  userId: string;
  name: string;
  email: string;
  authProvider: AuthProvider;
  dateOfBirth: string | null;
  age: number | null;
  phoneNumber: string | null;
  region: string | null;
  hasPassword: boolean;
  profileComplete: boolean;
  roles: string[];
}

/** Matches backend ProductStatus exactly (note: IN_ACTIVE, with underscore). */
export type ProductStatus = 'ACTIVE' | 'IN_ACTIVE' | 'OUT_OF_STOCK';

export interface Product {
  productId: number;
  productName: string;
  productDescription: string | null;
  price: number;
  currency: string | null;
  stockQuantity: number;
  status: ProductStatus;
  categoryName: string | null;
  brandName: string | null;
}

export interface CartItem {
  productId: number;
  productName: string;
  unitPrice: number;
  currency: string | null;
  quantity: number;
  lineTotal: number;
  stockQuantity: number;
  /** Highest quantity the selector may offer: min(stock, per-line limit). */
  maxQuantity: number;
  /** False when the product became unavailable or stock dropped below the quantity after it was added. */
  available: boolean;
}

export interface Cart {
  items: CartItem[];
  /** Units across available lines - what the navbar badge shows. */
  totalQuantity: number;
  /** Sum of available lines only. */
  subtotal: number;
  currency: string | null;
  hasUnavailableItems: boolean;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

/** Matches backend OrderStatus. */
export type OrderStatus =
  | 'SAVED_TO_CART'
  | 'PENDING_PAYMENT'
  | 'CONFIRMED'
  | 'IN_PROGRESS'
  | 'ON_HOLD'
  | 'CANCELED'
  | 'COMPLETED'
  | 'REQUEST_REFUND';

export interface OrderItem {
  quantity: number;
  unitPrice: number;
  productName: string;
  totalPrice: number | null;
}

/** GET /users/me/orders - CustomerOrderDto. */
export interface CustomerOrder {
  orderId: number;
  orderItems: OrderItem[];
  creationDate: string | null; // ISO local date-time
  updateDate: string | null;
  totalPrice: number | null;
  orderStatus: OrderStatus | null;
}

/** Matches backend AuditAction. */
export type AuditAction =
  | 'ACCOUNT_REGISTERED'
  | 'SIGNED_IN'
  | 'PROFILE_UPDATED'
  | 'PASSWORD_CHANGED'
  | 'ACCOUNT_DEACTIVATED'
  | 'ACCOUNT_REACTIVATED'
  | 'ACCOUNT_DELETED'
  | 'CART_ITEM_ADDED'
  | 'CART_ITEM_UPDATED'
  | 'CART_ITEM_REMOVED'
  | 'CART_CLEARED';

/** GET /users/me/activity - AuditLogResponse. */
export interface ActivityEntry {
  id: number;
  action: AuditAction;
  details: string | null;
  /** Email of whoever was signed in, or "system" (e.g. during sign-in / sign-up). */
  actor: string;
  occurredAt: string;
}
