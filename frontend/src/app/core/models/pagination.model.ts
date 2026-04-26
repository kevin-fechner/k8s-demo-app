export interface CursorPage<T> {
  data: T[];
  nextCursor: string | null;
  hasMore: boolean;
  size: number;
}
