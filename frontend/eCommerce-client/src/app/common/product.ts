import { ProductStatus } from "./product-status";

export class Product {

  constructor(
    public productId: number,
    public productName: string,
    public productDescription: string,
    public price: number,
    public currency: string,
    public stockQuantity: number,
    public status: ProductStatus,
    public categoryName: string | null,
    public brandName: string | null,
    public imageUrl: string
  ) { }

}