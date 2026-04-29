import { inject } from '@angular/core';
import { ResolveFn } from '@angular/router';
import { ProductService } from '../services/product.service';
import { Product } from '../models/product.model';
import { catchError, of } from 'rxjs';
import { CursorPage } from '../models/pagination.model';

export const productsResolver: ResolveFn<CursorPage<Product>> = () => {
  return inject(ProductService)
    .getPage()
    .pipe(catchError(() => of({ data: [], nextCursor: null, hasMore: false, size: 0 })));
};
