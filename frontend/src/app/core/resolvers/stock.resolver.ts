import {inject} from '@angular/core';
import {ResolveFn} from '@angular/router';
import {ProductService} from '../services/product.service';
import {catchError, of} from 'rxjs';
import {StockNumbers} from '../models/stock-numbers.model';

export const stockNumbersResolver: ResolveFn<StockNumbers> =
  () => {
    return inject(ProductService)
      .getStockNumbers()
      .pipe(catchError(() => of({total: 0, inStock: 0, outOfStock: 0})));
  };
