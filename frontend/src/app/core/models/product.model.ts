export interface Product {
  id: number;
  name: string;
  description: string;
  price: number;
  stock: number;
  createdAt: string;
  updatedAt: string;
}

export interface ProductRequest {
  name: string;
  description: string;
  price: number;
  stock: number;
}

export interface ProductFilter {
  name?: string;
  minPrice?: number;
  maxPrice?: number;
  inStock?: boolean;
}
