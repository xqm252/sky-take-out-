-- ============================================================
-- 苍穹外卖 样例数据生成脚本
-- 目标: 200 用户, 300 地址, 8 套餐, 3000 订单, ~8000 明细, 50 购物车
-- 用法: mysql -u root -p123456 < generate-sample-data.sql
-- 执行时间: 约 30-60 秒
-- ============================================================

SET NAMES utf8mb4;
USE sky_take_out;

-- 关闭检查以加速批量插入
SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;

-- ============================================================
-- Step 0: 清理旧数据 + 重置自增
-- ============================================================
DELETE FROM order_detail;
DELETE FROM orders;
DELETE FROM shopping_cart;
DELETE FROM address_book;
DELETE FROM user;
DELETE FROM setmeal_dish;
DELETE FROM setmeal;

ALTER TABLE user AUTO_INCREMENT = 1;
ALTER TABLE address_book AUTO_INCREMENT = 1;
ALTER TABLE orders AUTO_INCREMENT = 1;
ALTER TABLE order_detail AUTO_INCREMENT = 1;
ALTER TABLE setmeal AUTO_INCREMENT = 1;
ALTER TABLE setmeal_dish AUTO_INCREMENT = 1;
ALTER TABLE shopping_cart AUTO_INCREMENT = 1;

-- ============================================================
-- Step 1: 创建姓名/地址辅助表
-- ============================================================
DROP TEMPORARY TABLE IF EXISTS tmp_surname;
CREATE TEMPORARY TABLE tmp_surname (id INT AUTO_INCREMENT PRIMARY KEY, s VARCHAR(2));
INSERT INTO tmp_surname (s) VALUES
('张'),('李'),('王'),('刘'),('陈'),('杨'),('赵'),('黄'),('周'),('吴'),
('徐'),('孙'),('胡'),('朱'),('高'),('林'),('何'),('郭'),('马'),('罗'),
('梁'),('宋'),('郑'),('谢'),('韩'),('唐'),('冯'),('于'),('董'),('萧'),
('程'),('曹'),('袁'),('邓'),('许'),('傅'),('沈'),('曾'),('彭'),('吕'),
('苏'),('卢'),('蒋'),('蔡'),('贾'),('丁'),('魏'),('薛'),('叶'),('阎'),
('潘'),('杜'),('戴'),('夏'),('钟'),('汪'),('田'),('任'),('姜'),('范'),
('方'),('石'),('姚'),('谭'),('廖'),('邹'),('熊'),('金'),('陆'),('郝');

DROP TEMPORARY TABLE IF EXISTS tmp_given;
CREATE TEMPORARY TABLE tmp_given (id INT AUTO_INCREMENT PRIMARY KEY, c VARCHAR(1));
INSERT INTO tmp_given (c) VALUES
('伟'),('芳'),('娜'),('敏'),('静'),('丽'),('强'),('磊'),('军'),('洋'),
('勇'),('艳'),('杰'),('涛'),('明'),('超'),('秀'),('英'),('华'),('慧'),('鑫'),
('桂'),('权'),('文'),('斌'),('鹏'),('飞'),('宇'),('健'),('志'),('宏'),
('国'),('建'),('玉'),('春'),('小'),('海'),('晓'),('雪'),('婷'),('佳'),
('浩'),('然'),('雨'),('晨'),('阳'),('宁'),('琳'),('旭'),('瑞'),('冰'),
('倩'),('怡'),('爽'),('晶'),('欢'),('颖'),('思'),('萌'),('悦'),('彤'),
('翔'),('龙'),('凤'),('洁'),('玲'),('萍'),('瑜'),('波'),('凯'),('辉');

DROP TEMPORARY TABLE IF EXISTS tmp_city;
CREATE TEMPORARY TABLE tmp_city (
  id INT AUTO_INCREMENT PRIMARY KEY,
  province_code VARCHAR(12), province_name VARCHAR(32),
  city_code VARCHAR(12), city_name VARCHAR(32)
);
INSERT INTO tmp_city (province_code, province_name, city_code, city_name) VALUES
('110000','北京市','110100','北京市'),
('310000','上海市','310100','上海市'),
('440100','广东省','440100','广州市'),
('440300','广东省','440300','深圳市'),
('330100','浙江省','330100','杭州市'),
('510100','四川省','510100','成都市'),
('420100','湖北省','420100','武汉市'),
('320100','江苏省','320100','南京市');

DROP TEMPORARY TABLE IF EXISTS tmp_district;
CREATE TEMPORARY TABLE tmp_district (id INT AUTO_INCREMENT PRIMARY KEY, city_id INT, d VARCHAR(32));
INSERT INTO tmp_district (city_id, d) VALUES
(1,'朝阳区'),(1,'海淀区'),(1,'西城区'),(1,'东城区'),(1,'丰台区'),(1,'通州区'),(1,'大兴区'),
(2,'浦东新区'),(2,'徐汇区'),(2,'静安区'),(2,'黄浦区'),(2,'杨浦区'),(2,'闵行区'),(2,'长宁区'),
(3,'天河区'),(3,'越秀区'),(3,'海珠区'),(3,'白云区'),(3,'番禺区'),(3,'黄埔区'),
(4,'南山区'),(4,'福田区'),(4,'罗湖区'),(4,'宝安区'),(4,'龙岗区'),(4,'龙华区'),
(5,'西湖区'),(5,'拱墅区'),(5,'上城区'),(5,'滨江区'),(5,'余杭区'),(5,'萧山区'),
(6,'武侯区'),(6,'锦江区'),(6,'青羊区'),(6,'成华区'),(6,'金牛区'),(6,'高新区'),
(7,'武昌区'),(7,'江汉区'),(7,'洪山区'),(7,'江岸区'),(7,'硚口区'),(7,'汉阳区'),
(8,'玄武区'),(8,'鼓楼区'),(8,'秦淮区'),(8,'建邺区'),(8,'江宁区'),(8,'栖霞区');

DROP TEMPORARY TABLE IF EXISTS tmp_street;
CREATE TEMPORARY TABLE tmp_street (id INT AUTO_INCREMENT PRIMARY KEY, s VARCHAR(100));
INSERT INTO tmp_street (s) VALUES
('中山路'),('人民路'),('建设路'),('解放路'),('北京路'),('南京路'),('长安街'),
('和平路'),('文化路'),('科技路'),('高新路'),('滨河路'),('花园路'),('龙腾路'),
('幸福路'),('光明路'),('阳光大道'),('兴业路'),('创业路'),('学府路');

-- ============================================================
-- Step 2: 生成 200 用户
-- ============================================================
DROP PROCEDURE IF EXISTS gen_users;
DELIMITER //
CREATE PROCEDURE gen_users()
BEGIN
  DECLARE i INT DEFAULT 0;
  DECLARE v_name VARCHAR(32);
  DECLARE v_phone VARCHAR(11);
  DECLARE v_sex VARCHAR(2);
  DECLARE v_openid VARCHAR(45);
  DECLARE v_create_time DATETIME;
  DECLARE v_surname VARCHAR(2);
  DECLARE v_given VARCHAR(2);
  DECLARE v_start_ts BIGINT DEFAULT UNIX_TIMESTAMP('2026-02-01 00:00:00');
  DECLARE v_end_ts   BIGINT DEFAULT UNIX_TIMESTAMP('2026-08-01 00:00:00');

  WHILE i < 200 DO
    -- 随机姓 + 1~2个名
    SELECT s INTO v_surname FROM tmp_surname ORDER BY RAND() LIMIT 1;
    SELECT c INTO v_given FROM tmp_given ORDER BY RAND() LIMIT 1;
    SET v_name = CONCAT(v_surname, v_given);
    IF RAND() > 0.5 THEN
      SELECT c INTO v_given FROM tmp_given ORDER BY RAND() LIMIT 1;
      SET v_name = CONCAT(v_name, v_given);
    END IF;

    SET v_phone = CONCAT('1',
      ELT(FLOOR(1+RAND()*7),'30','35','36','37','38','39','32'),
      LPAD(FLOOR(RAND()*100000000), 8, '0'));
    SET v_sex = IF(RAND() > 0.48, '1', '0');
    SET v_openid = CONCAT('wx_openid_', LPAD(i+1, 8, '0'));
    SET v_create_time = FROM_UNIXTIME(v_start_ts + FLOOR(RAND() * (v_end_ts - v_start_ts)));

    INSERT INTO user (openid, name, phone, sex, create_time)
    VALUES (v_openid, v_name, v_phone, v_sex, v_create_time);

    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL gen_users();
DROP PROCEDURE IF EXISTS gen_users;

SELECT CONCAT('用户: ', COUNT(*), ' 条') AS progress FROM user;

-- ============================================================
-- Step 3: 生成地址簿 (~300 条, 每用户 1-3 个)
-- ============================================================
DROP PROCEDURE IF EXISTS gen_addresses;
DELIMITER //
CREATE PROCEDURE gen_addresses()
BEGIN
  DECLARE v_uid BIGINT DEFAULT 1;
  DECLARE v_user_count BIGINT;
  DECLARE v_addr_count INT;
  DECLARE v_is_default TINYINT;
  DECLARE j INT;
  DECLARE v_consignee VARCHAR(50);
  DECLARE v_phone VARCHAR(11);
  DECLARE v_province_code VARCHAR(12);
  DECLARE v_province_name VARCHAR(32);
  DECLARE v_city_code VARCHAR(12);
  DECLARE v_city_name VARCHAR(32);
  DECLARE v_district VARCHAR(32);
  DECLARE v_detail VARCHAR(200);
  DECLARE v_label VARCHAR(100);
  DECLARE v_city_id INT;
  DECLARE v_street VARCHAR(100);
  DECLARE v_building INT;
  DECLARE v_room INT;

  SELECT COUNT(*) INTO v_user_count FROM user;

  WHILE v_uid <= v_user_count DO
    SET v_addr_count = 1 + FLOOR(RAND() * 3); -- 每个用户 1-3 个地址

    SET j = 0;
    WHILE j < v_addr_count DO
      SET v_is_default = IF(j = 0, 1, 0); -- 第一个地址为默认

      -- 从 user 表取姓名和手机号
      SELECT name, phone INTO v_consignee, v_phone FROM user WHERE id = v_uid;

      -- 随机城市
      SELECT id, province_code, province_name, city_code, city_name
        INTO v_city_id, v_province_code, v_province_name, v_city_code, v_city_name
        FROM tmp_city ORDER BY RAND() LIMIT 1;

      -- 随机区
      SELECT d INTO v_district FROM tmp_district WHERE city_id = v_city_id ORDER BY RAND() LIMIT 1;

      -- 随机街道 + 小区
      SELECT s INTO v_street FROM tmp_street ORDER BY RAND() LIMIT 1;
      SET v_building = FLOOR(1 + RAND() * 120);
      SET v_room = FLOOR(101 + RAND() * 2500);
      SET v_detail = CONCAT(v_street, v_building, '号', v_room, '室');

      -- 随机标签
      SET v_label = ELT(FLOOR(1+RAND()*5), '家', '公司', '学校', '父母家', '');

      INSERT INTO address_book (user_id, consignee, sex, phone,
        province_code, province_name, city_code, city_name,
        district_code, district_name, detail, label, is_default)
      VALUES (v_uid, v_consignee, IF(RAND()>0.48,'1','0'), v_phone,
        v_province_code, v_province_name, v_city_code, v_city_name,
        CONCAT(v_city_code, '01'), v_district, v_detail,
        IF(v_label='', NULL, v_label), v_is_default);

      SET j = j + 1;
    END WHILE;

    SET v_uid = v_uid + 1;
  END WHILE;
END //
DELIMITER ;
CALL gen_addresses();
DROP PROCEDURE IF EXISTS gen_addresses;

SELECT CONCAT('地址: ', COUNT(*), ' 条') AS progress FROM address_book;

-- ============================================================
-- Step 4: 生成套餐 (8个) + 套餐菜品关联
-- ============================================================
INSERT INTO setmeal (category_id, name, price, status, description, create_time, update_time, create_user, update_user) VALUES
(13,'一人食套餐',     32.00, 1, '主菜+米饭+饮料，实惠美味',   '2026-03-01 10:00:00','2026-03-01 10:00:00',1,1),
(13,'双人烤鱼套餐',  128.00, 1, '烤鱼+2素菜+2米饭，两人共享','2026-03-01 10:00:00','2026-03-01 10:00:00',1,1),
(13,'牛蛙嗨吃套餐',   98.00, 1, '牛蛙+素菜+米饭，鲜香麻辣',  '2026-03-01 10:00:00','2026-03-01 10:00:00',1,1),
(13,'酸菜鱼套餐',     78.00, 1, '酸菜鱼+素菜+2米饭，经典搭配','2026-03-01 10:00:00','2026-03-01 10:00:00',1,1),
(15,'商务午餐',       68.00, 1, '蒸菜+素菜+米饭+汤，营养均衡','2026-03-15 10:00:00','2026-03-15 10:00:00',1,1),
(15,'家庭聚餐套餐',  188.00, 1, '大菜+2素+汤+3米饭，全家共享','2026-03-15 10:00:00','2026-03-15 10:00:00',1,1),
(15,'土豪尊享套餐',  228.00, 1, '东坡肘子+烤鱼+素菜+汤+米饭',  '2026-03-15 10:00:00','2026-03-15 10:00:00',1,1),
(13,'素食轻享套餐',   28.00, 1, '2素菜+米饭+饮料，清爽健康',  '2026-04-01 10:00:00','2026-04-01 10:00:00',1,1);

-- 套餐菜品关联 (setmeal_dish)
-- 1: 一人食套餐 = 米饭(49,2元) + 鸡蛋汤(68,4元) + 可选主菜随机(用蒜蓉娃娃菜55,18元), 总定价32
INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies) VALUES
(1,49,'米饭',2.00,1),
(1,68,'鸡蛋汤',4.00,1),
(1,55,'蒜蓉娃娃菜',18.00,1);

-- 2: 双人烤鱼套餐 = 草鱼2斤(65,68) + 清炒小油菜(54,18) + 炝炒圆白菜(57,18) + 2米饭(49,2), 定价128
INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies) VALUES
(2,65,'草鱼2斤',68.00,1),
(2,54,'清炒小油菜',18.00,1),
(2,57,'炝炒圆白菜',18.00,1),
(2,49,'米饭',2.00,2);

-- 3: 牛蛙嗨吃套餐 = 香锅牛蛙(63,88) + 清炒西兰花(56,18) + 米饭(49,2), 定价98
INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies) VALUES
(3,63,'香锅牛蛙',88.00,1),
(3,56,'清炒西兰花',18.00,1),
(3,49,'米饭',2.00,1);

-- 4: 酸菜鱼套餐 = 老坛酸菜鱼(51,56) + 蒜蓉娃娃菜(55,18) + 2米饭(49,2), 定价78
INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies) VALUES
(4,51,'老坛酸菜鱼',56.00,1),
(4,55,'蒜蓉娃娃菜',18.00,1),
(4,49,'米饭',2.00,2);

-- 5: 商务午餐 = 梅菜扣肉(60,58) + 清炒小油菜(54,18) + 米饭(49,2) + 鸡蛋汤(68,4), 定价68
INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies) VALUES
(5,60,'梅菜扣肉',58.00,1),
(5,54,'清炒小油菜',18.00,1),
(5,49,'米饭',2.00,1),
(5,68,'鸡蛋汤',4.00,1);

-- 6: 家庭聚餐套餐 = 东坡肘子(59,138) + 炝炒圆白菜(57,18) + 清炒西兰花(56,18) + 平菇豆腐汤(69,6) + 3米饭(49,2), 定价188
INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies) VALUES
(6,59,'东坡肘子',138.00,1),
(6,57,'炝炒圆白菜',18.00,1),
(6,56,'清炒西兰花',18.00,1),
(6,69,'平菇豆腐汤',6.00,1),
(6,49,'米饭',2.00,3);

-- 7: 土豪尊享套餐 = 东坡肘子(59,138) + 草鱼2斤(65,68) + 清炒小油菜(54,18) + 平菇豆腐汤(69,6) + 米饭(49,2), 定价228
INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies) VALUES
(7,59,'东坡肘子',138.00,1),
(7,65,'草鱼2斤',68.00,1),
(7,54,'清炒小油菜',18.00,1),
(7,69,'平菇豆腐汤',6.00,1),
(7,49,'米饭',2.00,1);

-- 8: 素食轻享套餐 = 蒜蓉娃娃菜(55,18) + 清炒西兰花(56,18) + 米饭(49,2) + 北冰洋(47,4), 定价28
INSERT INTO setmeal_dish (setmeal_id, dish_id, name, price, copies) VALUES
(8,55,'蒜蓉娃娃菜',18.00,1),
(8,56,'清炒西兰花',18.00,1),
(8,49,'米饭',2.00,1),
(8,47,'北冰洋',4.00,1);

SELECT CONCAT('套餐: ', COUNT(*), ' 条, 套餐明细: ', (SELECT COUNT(*) FROM setmeal_dish), ' 条') AS progress FROM setmeal;

-- ============================================================
-- Step 5: 生成订单 + 订单明细 (3000 订单, ~8000 明细)
-- 核心逻辑: 每单 1-5 道菜, 金额 = 菜品价格×数量汇总
-- ============================================================
DROP PROCEDURE IF EXISTS gen_orders;
DELIMITER //
CREATE PROCEDURE gen_orders()
BEGIN
  DECLARE v_order_count INT DEFAULT 0;
  DECLARE v_total_orders INT DEFAULT 3000;
  DECLARE v_user_id BIGINT;
  DECLARE v_addr_id BIGINT;
  DECLARE v_user_name VARCHAR(32);
  DECLARE v_user_phone VARCHAR(11);
  DECLARE v_consignee VARCHAR(50);
  DECLARE v_addr_detail VARCHAR(512);
  DECLARE v_order_time DATETIME;
  DECLARE v_status INT;
  DECLARE v_pay_status TINYINT;
  DECLARE v_pay_method INT;
  DECLARE v_amount DECIMAL(10,2);
  DECLARE v_order_number VARCHAR(50);
  DECLARE v_order_id BIGINT;
  DECLARE v_detail_count INT;
  DECLARE v_max_details INT;
  DECLARE v_dish_id BIGINT;
  DECLARE v_dish_name VARCHAR(32);
  DECLARE v_dish_price DECIMAL(10,2);
  DECLARE v_dish_image VARCHAR(255);
  DECLARE v_flavor VARCHAR(50);
  DECLARE v_qty INT;
  DECLARE v_detail_amount DECIMAL(10,2);
  DECLARE v_has_flavor INT;
  DECLARE v_cancel_reasons VARCHAR(255);
  DECLARE v_remark VARCHAR(100);
  DECLARE v_pack_amount INT;
  DECLARE v_tableware_number INT;
  DECLARE v_total_amount DECIMAL(10,2);
  DECLARE v_d INT;
  DECLARE v_start_ts BIGINT DEFAULT UNIX_TIMESTAMP('2026-02-01 00:00:00');
  DECLARE v_end_ts   BIGINT DEFAULT UNIX_TIMESTAMP('2026-08-10 00:00:00');
  DECLARE v_ts BIGINT;
  DECLARE v_weekday INT;
  DECLARE v_user_count BIGINT;
  DECLARE v_dish_count BIGINT;

  SELECT COUNT(*) INTO v_user_count FROM user;
  SELECT COUNT(*) INTO v_dish_count FROM dish;

  order_loop: WHILE v_order_count < v_total_orders DO
    -- 随机用户 + 他的地址
    SET v_user_id = FLOOR(1 + RAND() * v_user_count);
    SELECT a.id, a.consignee, a.phone, CONCAT(a.province_name, a.city_name, a.district_name, a.detail),
           u.name, u.phone
      INTO v_addr_id, v_consignee, v_user_phone, v_addr_detail, v_user_name, v_user_phone
      FROM address_book a JOIN user u ON a.user_id = u.id
      WHERE a.user_id = v_user_id ORDER BY RAND() LIMIT 1;

    -- 如果该用户没有地址, 重试
    IF v_addr_id IS NULL THEN
      ITERATE order_loop;
    END IF;

    -- 生成随机下单时间 (2026-02-01 ~ 2026-08-09)
    SET v_ts = v_start_ts + FLOOR(RAND() * (v_end_ts - v_start_ts));
    -- 周末订单量多30%: 如果是周末,提高通过率
    SET v_weekday = DAYOFWEEK(FROM_UNIXTIME(v_ts));
    IF (v_weekday = 1 OR v_weekday = 7) AND RAND() < 0.23 THEN
      -- 23% 概率跳过非周末订单 → 周末相对更多
      SET v_ts = v_ts;
    ELSEIF (v_weekday BETWEEN 2 AND 6) AND RAND() < 0.12 THEN
      SET v_ts = v_start_ts + FLOOR(RAND() * (v_end_ts - v_start_ts));
    END IF;

    -- 高峰时段加权: 午餐 11:00-13:00, 晚餐 17:00-20:00
    -- 用小时分布: 40% 午餐高峰, 35% 晚餐高峰, 25% 其他时间
    SET v_d = FLOOR(RAND() * 100);
    IF v_d < 40 THEN
      SET v_order_time = FROM_UNIXTIME(v_ts - FLOOR(RAND()*86400) + 39600 + FLOOR(RAND()*7200)); -- 11:00-13:00
    ELSEIF v_d < 75 THEN
      SET v_order_time = FROM_UNIXTIME(v_ts - FLOOR(RAND()*86400) + 61200 + FLOOR(RAND()*10800)); -- 17:00-20:00
    ELSE
      SET v_order_time = FROM_UNIXTIME(v_ts);
    END IF;

    -- 随机状态 (按比例)
    SET v_d = FLOOR(RAND() * 100);
    IF v_d < 60 THEN
      SET v_status = 5; -- 已完成 60%
    ELSEIF v_d < 75 THEN
      SET v_status = 6; -- 已取消 15%
    ELSEIF v_d < 85 THEN
      SET v_status = 4; -- 派送中 10%
    ELSEIF v_d < 90 THEN
      SET v_status = 3; -- 已接单 5%
    ELSEIF v_d < 95 THEN
      SET v_status = 2; -- 待接单 5%
    ELSE
      SET v_status = 1; -- 待付款 5%
    END IF;

    -- 支付状态
    IF v_status = 1 THEN
      SET v_pay_status = 0; -- 待付款 → 未支付
    ELSEIF v_status = 6 THEN
      SET v_pay_status = IF(RAND()>0.3, 1, 2); -- 取消 → 部分已退款
    ELSE
      SET v_pay_status = 1; -- 其他 → 已支付
    END IF;

    SET v_pay_method = IF(RAND() > 0.2, 1, 2); -- 80%微信 20%支付宝

    -- 订单号: yyyyMMddHHmmss + 6位随机
    SET v_order_number = CONCAT(
      DATE_FORMAT(v_order_time, '%Y%m%d%H%i%s'),
      LPAD(FLOOR(RAND()*1000000), 6, '0'));

    -- 打包费 0-5元
    SET v_pack_amount = FLOOR(RAND() * 6);
    SET v_tableware_number = 1 + FLOOR(RAND() * 4);

    -- 备注
    SET v_remark = CASE FLOOR(RAND()*8)
      WHEN 0 THEN '' WHEN 1 THEN '不要辣' WHEN 2 THEN '少放盐'
      WHEN 3 THEN '快点送达' WHEN 4 THEN '放门口' WHEN 5 THEN '多加香菜'
      WHEN 6 THEN '不要葱' ELSE '' END;
    IF v_remark = '' THEN SET v_remark = NULL; END IF;

    -- 取消原因 (仅取消订单)
    SET v_cancel_reasons = NULL;
    IF v_status = 6 THEN
      SET v_cancel_reasons = ELT(FLOOR(1+RAND()*5),
        '配送时间太长', '不想要了', '地址填写错误', '商品缺货', '其他原因');
    END IF;

    -- 插入订单
    INSERT INTO orders (number, status, user_id, address_book_id,
      order_time, checkout_time, pay_method, pay_status, amount,
      remark, phone, address, user_name, consignee,
      cancel_reason, cancel_time, estimated_delivery_time,
      delivery_status, delivery_time, pack_amount, tableware_number, tableware_status)
    VALUES (
      v_order_number, v_status, v_user_id, v_addr_id,
      v_order_time,
      IF(v_pay_status>=1, DATE_ADD(v_order_time, INTERVAL FLOOR(RAND()*600) SECOND), NULL),
      v_pay_method, v_pay_status, 0.00, -- amount 稍后更新
      v_remark, v_user_phone, v_addr_detail, v_user_name, v_consignee,
      v_cancel_reasons,
      IF(v_status=6, DATE_ADD(v_order_time, INTERVAL FLOOR(RAND()*1800) SECOND), NULL),
      IF(v_status>=3, DATE_ADD(v_order_time, INTERVAL 30 MINUTE), NULL),
      1, -- delivery_status
      IF(v_status=5, DATE_ADD(v_order_time, INTERVAL (20+FLOOR(RAND()*40)) MINUTE), NULL),
      v_pack_amount, v_tableware_number, 1
    );

    SET v_order_id = LAST_INSERT_ID();
    SET v_total_amount = 0;

    -- 生成订单明细 (1-5 条)
    SET v_max_details = 1 + FLOOR(RAND() * 5);
    IF v_max_details = 5 AND RAND() > 0.3 THEN SET v_max_details = 4; END IF; -- 5道菜少一些
    SET v_detail_count = 0;

    WHILE v_detail_count < v_max_details DO
      -- 随机选菜
      SELECT id, name, price, image INTO v_dish_id, v_dish_name, v_dish_price, v_dish_image
        FROM dish ORDER BY RAND() LIMIT 1;

      SET v_qty = 1 + FLOOR(RAND() * 3); -- 1-3份
      IF v_dish_price > 80 AND v_qty > 1 THEN SET v_qty = 1; END IF; -- 大菜通常只点1份
      SET v_detail_amount = v_dish_price * v_qty;

      -- 口味 (如果菜品有口味选项)
      SET v_flavor = NULL;
      SELECT COUNT(*) INTO v_has_flavor FROM dish_flavor WHERE dish_id = v_dish_id;
      IF v_has_flavor > 0 THEN
        SELECT value INTO v_flavor FROM dish_flavor WHERE dish_id = v_dish_id ORDER BY RAND() LIMIT 1;
      END IF;

      INSERT INTO order_detail (name, image, order_id, dish_id, setmeal_id, dish_flavor, number, amount)
      VALUES (v_dish_name, v_dish_image, v_order_id, v_dish_id, NULL, v_flavor, v_qty, v_detail_amount);

      SET v_total_amount = v_total_amount + v_detail_amount;
      SET v_detail_count = v_detail_count + 1;
    END WHILE;

    -- 更新订单总金额 = 明细合计 + 打包费
    UPDATE orders SET amount = v_total_amount + v_pack_amount WHERE id = v_order_id;

    SET v_order_count = v_order_count + 1;
    IF v_order_count % 500 = 0 THEN
      SELECT CONCAT('订单进度: ', v_order_count, '/', v_total_orders) AS progress;
    END IF;
  END WHILE;
END //
DELIMITER ;

CALL gen_orders();
DROP PROCEDURE IF EXISTS gen_orders;

SELECT CONCAT('订单: ', COUNT(*), ' 条, 明细: ', (SELECT COUNT(*) FROM order_detail), ' 条') AS progress FROM orders;

-- ============================================================
-- Step 6: 生成购物车 (~50 条, 部分活跃用户有未下单商品)
-- ============================================================
DROP PROCEDURE IF EXISTS gen_cart;
DELIMITER //
CREATE PROCEDURE gen_cart()
BEGIN
  DECLARE i INT DEFAULT 0;
  DECLARE v_uid BIGINT;
  DECLARE v_dish_id BIGINT;
  DECLARE v_qty INT;
  DECLARE v_dish_name VARCHAR(32);
  DECLARE v_dish_price DECIMAL(10,2);
  DECLARE v_dish_image VARCHAR(255);

  WHILE i < 50 DO
    SET v_uid = FLOOR(1 + RAND() * 200);
    SELECT id, name, price, image INTO v_dish_id, v_dish_name, v_dish_price, v_dish_image
      FROM dish ORDER BY RAND() LIMIT 1;
    SET v_qty = 1 + FLOOR(RAND() * 3);

    INSERT INTO shopping_cart (name, image, user_id, dish_id, setmeal_id, dish_flavor, number, amount, create_time)
    VALUES (v_dish_name, v_dish_image, v_uid, v_dish_id, NULL, NULL, v_qty,
      v_dish_price * v_qty, DATE_SUB(NOW(), INTERVAL FLOOR(RAND()*72) HOUR));

    SET i = i + 1;
  END WHILE;
END //
DELIMITER ;
CALL gen_cart();
DROP PROCEDURE IF EXISTS gen_cart;

SELECT CONCAT('购物车: ', COUNT(*), ' 条') AS progress FROM shopping_cart;

-- ============================================================
-- Step 7: 修复 AUTO_INCREMENT
-- ============================================================
-- 由于我们手动插入了 ID=1 开始的行，需要修正自增值
SELECT MAX(id)+1 INTO @next_id FROM user;
SET @sql = CONCAT('ALTER TABLE user AUTO_INCREMENT = ', @next_id);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT MAX(id)+1 INTO @next_id FROM address_book;
SET @sql = CONCAT('ALTER TABLE address_book AUTO_INCREMENT = ', @next_id);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT MAX(id)+1 INTO @next_id FROM orders;
SET @sql = CONCAT('ALTER TABLE orders AUTO_INCREMENT = ', @next_id);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT MAX(id)+1 INTO @next_id FROM order_detail;
SET @sql = CONCAT('ALTER TABLE order_detail AUTO_INCREMENT = ', @next_id);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT MAX(id)+1 INTO @next_id FROM setmeal;
SET @sql = CONCAT('ALTER TABLE setmeal AUTO_INCREMENT = ', @next_id);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT MAX(id)+1 INTO @next_id FROM setmeal_dish;
SET @sql = CONCAT('ALTER TABLE setmeal_dish AUTO_INCREMENT = ', @next_id);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

SELECT MAX(id)+1 INTO @next_id FROM shopping_cart;
SET @sql = CONCAT('ALTER TABLE shopping_cart AUTO_INCREMENT = ', @next_id);
PREPARE stmt FROM @sql; EXECUTE stmt; DEALLOCATE PREPARE stmt;

-- 提交
COMMIT;

-- 恢复检查
SET unique_checks = 1;
SET foreign_key_checks = 1;
SET autocommit = 1;

-- ============================================================
-- Step 8: 数据验证
-- ============================================================
SELECT '========================================' AS '';
SELECT '        样例数据生成完毕 - 验证报告' AS '';
SELECT '========================================' AS '';

-- 各表行数
SELECT '📊 各表行数统计' AS '';
SELECT 'user' AS 表名, COUNT(*) AS 行数 FROM user
UNION ALL SELECT 'address_book', COUNT(*) FROM address_book
UNION ALL SELECT 'orders', COUNT(*) FROM orders
UNION ALL SELECT 'order_detail', COUNT(*) FROM order_detail
UNION ALL SELECT 'setmeal', COUNT(*) FROM setmeal
UNION ALL SELECT 'setmeal_dish', COUNT(*) FROM setmeal_dish
UNION ALL SELECT 'shopping_cart', COUNT(*) FROM shopping_cart
UNION ALL SELECT 'category', COUNT(*) FROM category
UNION ALL SELECT 'dish', COUNT(*) FROM dish
UNION ALL SELECT 'employee', COUNT(*) FROM employee;

-- 订单状态分布
SELECT '📊 订单状态分布' AS '';
SELECT
  CASE status
    WHEN 1 THEN '1-待付款' WHEN 2 THEN '2-待接单' WHEN 3 THEN '3-已接单'
    WHEN 4 THEN '4-派送中' WHEN 5 THEN '5-已完成' WHEN 6 THEN '6-已取消'
    WHEN 7 THEN '7-退款' ELSE CONCAT(status, '-未知')
  END AS 状态,
  COUNT(*) AS 数量,
  CONCAT(ROUND(COUNT(*)*100.0/(SELECT COUNT(*) FROM orders),1),'%') AS 占比
FROM orders GROUP BY status ORDER BY status;

-- 订单时间分布 (按月)
SELECT '📊 月度订单分布' AS '';
SELECT DATE_FORMAT(order_time, '%Y-%m') AS 月份, COUNT(*) AS 订单数,
  ROUND(AVG(amount),1) AS 平均金额
FROM orders GROUP BY 月份 ORDER BY 月份;

-- 金额分布
SELECT '📊 订单金额分布' AS '';
SELECT
  CASE WHEN amount < 20 THEN '  <20元'
       WHEN amount < 50 THEN '20-50元'
       WHEN amount < 100 THEN '50-100元'
       WHEN amount < 200 THEN '100-200元'
       ELSE '  ≥200元' END AS 金额区间,
  COUNT(*) AS 订单数,
  CONCAT(ROUND(COUNT(*)*100.0/(SELECT COUNT(*) FROM orders),1),'%') AS 占比
FROM orders GROUP BY 金额区间 ORDER BY MIN(amount);

-- Top10 畅销菜品
SELECT '📊 畅销菜品 Top10' AS '';
SELECT d.name AS 菜品, SUM(od.number) AS 总销量,
  ROUND(SUM(od.amount),0) AS 总金额
FROM order_detail od JOIN dish d ON od.dish_id = d.id
GROUP BY od.dish_id, d.name
ORDER BY 总销量 DESC LIMIT 10;

-- 用户消费排行 Top5
SELECT '📊 消费金额 Top5 用户' AS '';
SELECT u.name AS 用户, COUNT(o.id) AS 订单数,
  ROUND(SUM(o.amount),0) AS 总消费
FROM orders o JOIN user u ON o.user_id = u.id
GROUP BY o.user_id, u.name
ORDER BY 总消费 DESC LIMIT 5;
