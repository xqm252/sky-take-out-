-- ============================================================
-- 苍穹外卖 Hive 外表建表脚本
-- 数据源: Sqoop 全量导入 HDFS，逗号分隔，无表头
-- 数据库: sky_take_out
-- ============================================================

USE sky_take_out;

-- 1. address_book（用户地址簿）
DROP TABLE IF EXISTS address_book;
CREATE EXTERNAL TABLE address_book (
  id              BIGINT,
  user_id         BIGINT,
  consignee       STRING,
  sex             STRING,
  phone           STRING,
  province_code   STRING,
  province_name   STRING,
  city_code       STRING,
  city_name       STRING,
  district_code   STRING,
  district_name   STRING,
  detail          STRING,
  label           STRING,
  is_default      TINYINT
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/address_book';

-- 2. category（分类）
DROP TABLE IF EXISTS category;
CREATE EXTERNAL TABLE category (
  id          BIGINT,
  type        INT,
  name        STRING,
  sort        INT,
  status      INT,
  create_time STRING,
  update_time STRING,
  create_user BIGINT,
  update_user BIGINT
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/category';

-- 3. dish（菜品）
DROP TABLE IF EXISTS dish;
CREATE EXTERNAL TABLE dish (
  id          BIGINT,
  name        STRING,
  category_id BIGINT,
  price       DECIMAL(10,2),
  image       STRING,
  description STRING,
  status      INT,
  create_time STRING,
  update_time STRING,
  create_user BIGINT,
  update_user BIGINT
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/dish';

-- 4. dish_flavor（菜品口味）
DROP TABLE IF EXISTS dish_flavor;
CREATE EXTERNAL TABLE dish_flavor (
  id      BIGINT,
  dish_id BIGINT,
  name    STRING,
  value   STRING
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/dish_flavor';

-- 5. employee（员工）
DROP TABLE IF EXISTS employee;
CREATE EXTERNAL TABLE employee (
  id          BIGINT,
  name        STRING,
  username    STRING,
  password    STRING,
  phone       STRING,
  sex         STRING,
  id_number   STRING,
  status      INT,
  create_time STRING,
  update_time STRING,
  create_user BIGINT,
  update_user BIGINT
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/employee';

-- 6. orders（订单——核心表）
DROP TABLE IF EXISTS orders;
CREATE EXTERNAL TABLE orders (
  id                      BIGINT,
  number                  STRING,
  status                  INT,
  user_id                 BIGINT,
  address_book_id         BIGINT,
  order_time              STRING,
  checkout_time           STRING,
  pay_method              INT,
  pay_status              TINYINT,
  amount                  DECIMAL(10,2),
  remark                  STRING,
  phone                   STRING,
  address                 STRING,
  user_name               STRING,
  consignee               STRING,
  cancel_reason           STRING,
  rejection_reason        STRING,
  cancel_time             STRING,
  estimated_delivery_time STRING,
  delivery_status         TINYINT,
  delivery_time           STRING,
  pack_amount             INT,
  tableware_number        INT,
  tableware_status        TINYINT
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/orders';

-- 7. order_detail（订单明细）
DROP TABLE IF EXISTS order_detail;
CREATE EXTERNAL TABLE order_detail (
  id          BIGINT,
  name        STRING,
  image       STRING,
  order_id    BIGINT,
  dish_id     BIGINT,
  setmeal_id  BIGINT,
  dish_flavor STRING,
  number      INT,
  amount      DECIMAL(10,2)
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/order_detail';

-- 8. setmeal（套餐）
DROP TABLE IF EXISTS setmeal;
CREATE EXTERNAL TABLE setmeal (
  id          BIGINT,
  category_id BIGINT,
  name        STRING,
  price       DECIMAL(10,2),
  status      INT,
  description STRING,
  image       STRING,
  create_time STRING,
  update_time STRING,
  create_user BIGINT,
  update_user BIGINT
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/setmeal';

-- 9. setmeal_dish（套餐-菜品关联）
DROP TABLE IF EXISTS setmeal_dish;
CREATE EXTERNAL TABLE setmeal_dish (
  id         BIGINT,
  setmeal_id BIGINT,
  dish_id    BIGINT,
  name       STRING,
  price      DECIMAL(10,2),
  copies     INT
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/setmeal_dish';

-- 10. shopping_cart（购物车）
DROP TABLE IF EXISTS shopping_cart;
CREATE EXTERNAL TABLE shopping_cart (
  id          BIGINT,
  name        STRING,
  image       STRING,
  user_id     BIGINT,
  dish_id     BIGINT,
  setmeal_id  BIGINT,
  dish_flavor STRING,
  number      INT,
  amount      DECIMAL(10,2),
  create_time STRING
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/shopping_cart';

-- 11. user（C端用户）
DROP TABLE IF EXISTS "user";
CREATE EXTERNAL TABLE "user" (
  id          BIGINT,
  openid      STRING,
  name        STRING,
  phone       STRING,
  sex         STRING,
  id_number   STRING,
  avatar      STRING,
  create_time STRING
)
ROW FORMAT DELIMITED FIELDS TERMINATED BY ','
STORED AS TEXTFILE
LOCATION '/user/hive/warehouse/sky_take_out.db/user';
