export type OrderStatus = 'PENDING' | 'CONFIRMED' | 'SHIPPED' | 'DELIVERED' | 'CANCELLED';

export interface OrderItem {
  id: number;
  productId: number;
  productName: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
}

export interface Order {
  id: number;
  customerName: string;
  customerEmail: string;
  status: OrderStatus;
  totalAmount: number;
  notes: string;
  items: OrderItem[];
  createdAt: string;
  updatedAt: string;
}

export interface OrderItemRequest {
  productId: number;
  quantity: number;
}

export interface OrderRequest {
  customerName: string;
  customerEmail: string;
  items: OrderItemRequest[];
  notes?: string;
}

export interface UpdateStatusRequest {
  status: OrderStatus;
}

export interface OrderFilter {
  status?: OrderStatus;
  customerEmail?: string;
  fromDate?: string;
  toDate?: string;
}
