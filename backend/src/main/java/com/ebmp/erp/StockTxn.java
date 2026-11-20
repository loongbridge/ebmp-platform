package com.ebmp.erp;

import com.ebmp.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.Transient;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "erp_stock_txn")
public class StockTxn extends BaseEntity {
    @Column(nullable = false, unique = true, length = 50)
    private String txnNo;

    /** IN 入库 / OUT 出库 */
    @Column(nullable = false, length = 10)
    private String type;

    @Column(nullable = false)
    private Long materialId;

    @Column(nullable = false)
    private Long warehouseId;

    private BigDecimal quantity = BigDecimal.ZERO;

    private BigDecimal beforeQuantity = BigDecimal.ZERO;

    private BigDecimal afterQuantity = BigDecimal.ZERO;

    /** PURCHASE_IN / SALES_OUT / ADJUST */
    private String bizType;

    private String bizNo;

    private Long operatorId;

    private String remark;

    @Transient
    private String materialName;

    @Transient
    private String materialCode;

    @Transient
    private String unit;

    @Transient
    private String warehouseName;

    @Transient
    private String operatorName;
}
