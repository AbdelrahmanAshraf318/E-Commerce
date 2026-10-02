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
