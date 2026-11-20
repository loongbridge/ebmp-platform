-- =====================================================================
--  EBMP 企业商业管理平台 · MySQL 生产库初始化脚本
--  版本：v1.0.0   适用：MySQL 8.0+
--
--  执行方式：
--    1) 命令行：mysql -uroot -p < init-mysql.sql
--    2) 客户端：登录后执行  source /路径/init-mysql.sql
--
--  脚本内容：
--    1. 创建数据库 ebmp（utf8mb4）
--    2. 创建应用专用账号 ebmp 并授权（生产建议，勿用 root）
--    3. 创建 34 张业务表（IF NOT EXISTS，可重复执行）
--    4. 唯一约束内联在建表语句中，保证脚本幂等
--
--  注意：
--    · 基线数据（角色/权限/部门/默认账号）由后端启动时 DataSeeder 自动写入
--    · 建表语句由 Hibernate 实体模型生成，与应用代码完全一致
--    · 仅快速建表时，也可直接以 profile=mysql 启动后端（ddl-auto=update 自动建表）
--    · 首次执行后请修改 ebmp 账号密码，并同步后端 MYSQL_PASSWORD
-- =====================================================================

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ---------- 1. 数据库 ----------
CREATE DATABASE IF NOT EXISTS `ebmp`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

-- ---------- 2. 应用账号 ----------
CREATE USER IF NOT EXISTS 'ebmp'@'%' IDENTIFIED BY 'Ebmp_ChangeMe_2026';
ALTER USER 'ebmp'@'%' IDENTIFIED BY 'Ebmp_ChangeMe_2026';
GRANT SELECT, INSERT, UPDATE, DELETE, CREATE, ALTER, INDEX, REFERENCES ON `ebmp`.* TO 'ebmp'@'%';
FLUSH PRIVILEGES;

USE `ebmp`;

-- ---------- 3. 业务表 ----------

-- 主数据（客户 / 供应商 / 物料 / 仓库 / 联系人）
CREATE TABLE IF NOT EXISTS md_contact (is_primary integer, created_at datetime(6), customer_id bigint not null, id bigint not null auto_increment, updated_at datetime(6), name varchar(50) not null, email varchar(255), phone varchar(255), position varchar(255), remark varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS md_customer (credit_limit decimal(38,2), status integer, created_at datetime(6), id bigint not null auto_increment, last_contact_at datetime(6), owner_id bigint, updated_at datetime(6), code varchar(50) not null, name varchar(100) not null, address varchar(255), email varchar(255), industry varchar(255), level varchar(255), phone varchar(255), remark varchar(255), short_name varchar(255), source varchar(255), tax_no varchar(255), CONSTRAINT UK_dlbuhsw12dsdvgygs5rmu6djp UNIQUE (code), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS md_material (cost_price decimal(38,2), safe_stock decimal(38,2), sale_price decimal(38,2), status integer, created_at datetime(6), id bigint not null auto_increment, updated_at datetime(6), unit varchar(20) not null, code varchar(50) not null, name varchar(100) not null, category varchar(255), remark varchar(255), spec varchar(255), CONSTRAINT UK_4rv3d3mah8qjm5lht6wbhqx1i UNIQUE (code), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS md_supplier (cooperation_start date, score decimal(38,2), status integer, created_at datetime(6), id bigint not null auto_increment, updated_at datetime(6), code varchar(50) not null, name varchar(100) not null, address varchar(255), bank_account varchar(255), bank_name varchar(255), category varchar(255), contact_name varchar(255), email varchar(255), level varchar(255), phone varchar(255), remark varchar(255), short_name varchar(255), tax_no varchar(255), CONSTRAINT UK_7rijg7bcjwlr7o8f58amjfvcf UNIQUE (code), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS md_warehouse (status integer, created_at datetime(6), id bigint not null auto_increment, manager_id bigint, updated_at datetime(6), code varchar(50) not null, name varchar(80) not null, location varchar(255), remark varchar(255), CONSTRAINT UK_4l6n27grqsjs5mswytlyc4uhb UNIQUE (code), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 系统与权限（用户 / 角色 / 权限 / 部门）
CREATE TABLE IF NOT EXISTS sys_dept (sort integer, status integer, created_at datetime(6), id bigint not null auto_increment, leader_id bigint, parent_id bigint, updated_at datetime(6), name varchar(50) not null, primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS sys_permission (sort integer, created_at datetime(6), id bigint not null auto_increment, parent_id bigint, updated_at datetime(6), name varchar(50) not null, code varchar(80), icon varchar(255), path varchar(255), type varchar(255), CONSTRAINT UK_2vm98en2ouht0v15fvef2whp4 UNIQUE (code), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS sys_role (data_scope integer, status integer, created_at datetime(6), id bigint not null auto_increment, updated_at datetime(6), code varchar(50) not null, name varchar(50) not null, description varchar(255), CONSTRAINT UK_plpigyqwsqfn7mn66npgf9ftp UNIQUE (code), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS sys_role_permission (created_at datetime(6), id bigint not null auto_increment, permission_id bigint not null, role_id bigint not null, updated_at datetime(6), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS sys_user (status integer, created_at datetime(6), dept_id bigint, id bigint not null auto_increment, last_login_at datetime(6), updated_at datetime(6), phone varchar(20), real_name varchar(50) not null, username varchar(50) not null, email varchar(100), password varchar(100) not null, avatar varchar(255), position varchar(255), CONSTRAINT UK_51bvuyvihefoh4kp5syh2jpi4 UNIQUE (username), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS sys_user_role (created_at datetime(6), id bigint not null auto_increment, role_id bigint not null, updated_at datetime(6), user_id bigint not null, primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- OA 协同办公
CREATE TABLE IF NOT EXISTS oa_attendance (work_date date not null, check_in_time datetime(6), check_out_time datetime(6), created_at datetime(6), id bigint not null auto_increment, updated_at datetime(6), user_id bigint, remark varchar(255), status varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS oa_document (created_at datetime(6), file_size bigint, id bigint not null auto_increment, updated_at datetime(6), uploader_id bigint, name varchar(200) not null, category varchar(255), file_path varchar(255), file_url varchar(255), remark varchar(255), version varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS oa_leave (days decimal(38,2), approval_id bigint, created_at datetime(6), end_time datetime(6), id bigint not null auto_increment, start_time datetime(6), updated_at datetime(6), user_id bigint, leave_no varchar(50) not null, reason varchar(255), status varchar(255), type varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS oa_meeting (created_at datetime(6), end_time datetime(6), id bigint not null auto_increment, organizer_id bigint, start_time datetime(6), updated_at datetime(6), title varchar(200) not null, remark varchar(255), room varchar(255), status varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS oa_meeting_attendee (created_at datetime(6), id bigint not null auto_increment, meeting_id bigint not null, updated_at datetime(6), user_id bigint not null, primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS oa_notice (status integer, views integer, created_at datetime(6), id bigint not null auto_increment, publish_time datetime(6), publisher_id bigint, updated_at datetime(6), title varchar(200) not null, content varchar(5000), category varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS oa_schedule (remind_minutes integer, created_at datetime(6), end_time datetime(6), id bigint not null auto_increment, owner_id bigint, start_time datetime(6), updated_at datetime(6), title varchar(200) not null, description varchar(255), location varchar(255), status varchar(255), type varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS oa_task (due_date date, assignee_id bigint, completed_at datetime(6), created_at datetime(6), creator_id bigint, id bigint not null auto_increment, updated_at datetime(6), title varchar(200) not null, description varchar(1000), priority varchar(255), status varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- CRM 客户关系
CREATE TABLE IF NOT EXISTS crm_activity (activity_time datetime(6), biz_id bigint not null, created_at datetime(6), id bigint not null auto_increment, owner_id bigint, updated_at datetime(6), biz_type varchar(20) not null, content varchar(1000), next_plan varchar(255), type varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS crm_lead (created_at datetime(6), customer_id bigint, id bigint not null auto_increment, next_follow_at datetime(6), opportunity_id bigint, owner_id bigint, updated_at datetime(6), lead_no varchar(50) not null, name varchar(100) not null, company varchar(255), contact_name varchar(255), email varchar(255), phone varchar(255), remark varchar(255), source varchar(255), status varchar(255), CONSTRAINT UK_be6l11hcrpymev6t7qvxm7100 UNIQUE (lead_no), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS crm_opportunity (amount decimal(38,2), expected_close_date date, probability integer, created_at datetime(6), customer_id bigint not null, id bigint not null auto_increment, lead_id bigint, owner_id bigint, updated_at datetime(6), won_at datetime(6), opp_no varchar(50) not null, name varchar(120) not null, remark varchar(255), stage varchar(255), status varchar(255), CONSTRAINT UK_7al6bmiiprmesf145m7638wgv UNIQUE (opp_no), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- SRM 供应商协同
CREATE TABLE IF NOT EXISTS srm_purchase_order (expected_date date, total_amount decimal(38,2), approval_id bigint, created_at datetime(6), creator_id bigint, id bigint not null auto_increment, requisition_id bigint, supplier_id bigint not null, updated_at datetime(6), order_no varchar(50) not null, remark varchar(255), status varchar(255), CONSTRAINT UK_cev4mtif712635pgql2vdeeir UNIQUE (order_no), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS srm_purchase_order_item (amount decimal(38,2), price decimal(38,2), quantity decimal(38,2), received_qty decimal(38,2), created_at datetime(6), id bigint not null auto_increment, material_id bigint not null, order_id bigint not null, updated_at datetime(6), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS srm_requisition (need_date date, total_amount decimal(38,2), applicant_id bigint, approval_id bigint, created_at datetime(6), dept_id bigint, id bigint not null auto_increment, updated_at datetime(6), req_no varchar(50) not null, title varchar(200) not null, remark varchar(255), status varchar(255), CONSTRAINT UK_1u7ayppmoxckevfncwrh7ac1a UNIQUE (req_no), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS srm_requisition_item (amount decimal(38,2), est_price decimal(38,2), quantity decimal(38,2), created_at datetime(6), id bigint not null auto_increment, material_id bigint not null, requisition_id bigint not null, updated_at datetime(6), remark varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS srm_supplier_evaluation (delivery_score decimal(38,2), price_score decimal(38,2), quality_score decimal(38,2), service_score decimal(38,2), total_score decimal(38,2), created_at datetime(6), evaluator_id bigint, id bigint not null auto_increment, supplier_id bigint not null, updated_at datetime(6), period varchar(20) not null, comment varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 进销存
CREATE TABLE IF NOT EXISTS erp_sales_order (order_date date, total_amount decimal(38,2), approval_id bigint, contract_id bigint, created_at datetime(6), creator_id bigint, customer_id bigint not null, id bigint not null auto_increment, opportunity_id bigint, updated_at datetime(6), order_no varchar(50) not null, remark varchar(255), status varchar(255), CONSTRAINT UK_r44o9ip760lxjfat19g9v1s57 UNIQUE (order_no), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS erp_sales_order_item (amount decimal(38,2), price decimal(38,2), quantity decimal(38,2), shipped_qty decimal(38,2), created_at datetime(6), id bigint not null auto_increment, material_id bigint not null, order_id bigint not null, updated_at datetime(6), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS erp_stock (locked_quantity decimal(38,2), quantity decimal(38,2), created_at datetime(6), id bigint not null auto_increment, material_id bigint not null, updated_at datetime(6), warehouse_id bigint not null, CONSTRAINT UKl7do442rood27ls98idwlry7x UNIQUE (material_id, warehouse_id), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS erp_stock_txn (after_quantity decimal(38,2), before_quantity decimal(38,2), quantity decimal(38,2), created_at datetime(6), id bigint not null auto_increment, material_id bigint not null, operator_id bigint, updated_at datetime(6), warehouse_id bigint not null, type varchar(10) not null, txn_no varchar(50) not null, biz_no varchar(255), biz_type varchar(255), remark varchar(255), CONSTRAINT UK_rwiuqm14wmol55cgvtmw7bpj9 UNIQUE (txn_no), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 合同与审批
CREATE TABLE IF NOT EXISTS biz_contract (amount decimal(38,2), end_date date, start_date date, approval_id bigint, created_at datetime(6), id bigint not null auto_increment, opportunity_id bigint, owner_id bigint, party_id bigint not null, sales_order_id bigint, signed_at datetime(6), updated_at datetime(6), type varchar(20) not null, contract_no varchar(50) not null, title varchar(200) not null, remark varchar(255), status varchar(255), CONSTRAINT UK_damksy7xi7hopxv6r7yu40e5h UNIQUE (contract_no), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS wf_approval (amount decimal(38,2), current_step integer, total_steps integer, applicant_id bigint, biz_id bigint, created_at datetime(6), finished_at datetime(6), id bigint not null auto_increment, updated_at datetime(6), module varchar(20) not null, status varchar(20) not null, biz_type varchar(40) not null, applicant_name varchar(50), biz_no varchar(50), current_node_name varchar(50), title varchar(200) not null, remark varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS wf_approval_node (step_no integer not null, acted_at datetime(6), approval_id bigint not null, approver_id bigint, created_at datetime(6), id bigint not null auto_increment, updated_at datetime(6), status varchar(20) not null, approver_name varchar(50), node_name varchar(50) not null, comment varchar(255), primary key (id)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
SET FOREIGN_KEY_CHECKS = 1;

-- ---------- 4. 校验 ----------
-- 期望 table_count = 34
SELECT COUNT(*) AS table_count FROM information_schema.tables
  WHERE table_schema = 'ebmp' AND table_type = 'BASE TABLE';

-- ---------- 5. 后端连接示例 ----------
-- java -jar ebmp-backend.jar --spring.profiles.active=mysql \
--   --spring.datasource.url="jdbc:mysql://127.0.0.1:3306/ebmp?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&allowPublicKeyRetrieval=true&useSSL=false" \
--   --spring.datasource.username=ebmp \
--   --spring.datasource.password=Ebmp_ChangeMe_2026
