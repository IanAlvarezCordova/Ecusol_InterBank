package com.ecusol.ms_transacciones.dto.iso;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.Serializable;
import java.math.BigDecimal;

// --- ROOT ---
public class IsoMensajeDTO implements Serializable {
    @JsonProperty("header")
    private IsoHeader header;

    @JsonProperty("body")
    private IsoBody body;

    public IsoMensajeDTO() {
    }

    public IsoMensajeDTO(IsoHeader header, IsoBody body) {
        this.header = header;
        this.body = body;
    }

    public IsoHeader getHeader() {
        return header;
    }

    public void setHeader(IsoHeader header) {
        this.header = header;
    }

    public IsoBody getBody() {
        return body;
    }

    public void setBody(IsoBody body) {
        this.body = body;
    }

    // --- NESTED CLASSES ---

    public static class IsoHeader implements Serializable {
        private String messageId;
        private String creationDateTime;
        private String originatingBankId;

        public IsoHeader() {
        }

        public IsoHeader(String messageId, String creationDateTime, String originatingBankId) {
            this.messageId = messageId;
            this.creationDateTime = creationDateTime;
            this.originatingBankId = originatingBankId;
        }

        public String getMessageId() {
            return messageId;
        }

        public void setMessageId(String messageId) {
            this.messageId = messageId;
        }

        public String getCreationDateTime() {
            return creationDateTime;
        }

        public void setCreationDateTime(String creationDateTime) {
            this.creationDateTime = creationDateTime;
        }

        public String getOriginatingBankId() {
            return originatingBankId;
        }

        public void setOriginatingBankId(String originatingBankId) {
            this.originatingBankId = originatingBankId;
        }
    }

    public static class IsoBody implements Serializable {
        private String instructionId;
        private String endToEndId;
        private IsoAmount amount;
        private IsoDebtor debtor;
        private IsoCreditor creditor;
        private String remittanceInformation;

        public String getInstructionId() {
            return instructionId;
        }

        public void setInstructionId(String instructionId) {
            this.instructionId = instructionId;
        }

        public String getEndToEndId() {
            return endToEndId;
        }

        public void setEndToEndId(String endToEndId) {
            this.endToEndId = endToEndId;
        }

        public IsoAmount getAmount() {
            return amount;
        }

        public void setAmount(IsoAmount amount) {
            this.amount = amount;
        }

        public IsoDebtor getDebtor() {
            return debtor;
        }

        public void setDebtor(IsoDebtor debtor) {
            this.debtor = debtor;
        }

        public IsoCreditor getCreditor() {
            return creditor;
        }

        public void setCreditor(IsoCreditor creditor) {
            this.creditor = creditor;
        }

        public String getRemittanceInformation() {
            return remittanceInformation;
        }

        public void setRemittanceInformation(String remittanceInformation) {
            this.remittanceInformation = remittanceInformation;
        }
    }

    public static class IsoAmount implements Serializable {
        private String currency;
        private BigDecimal value;

        public IsoAmount() {
        }

        public IsoAmount(String currency, BigDecimal value) {
            this.currency = currency;
            this.value = value;
        }

        public String getCurrency() {
            return currency;
        }

        public void setCurrency(String currency) {
            this.currency = currency;
        }

        public BigDecimal getValue() {
            return value;
        }

        public void setValue(BigDecimal value) {
            this.value = value;
        }
    }

    public static class IsoDebtor implements Serializable {
        private String name;
        private String accountId;
        private String accountType;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getAccountType() {
            return accountType;
        }

        public void setAccountType(String accountType) {
            this.accountType = accountType;
        }
    }

    public static class IsoCreditor implements Serializable {
        private String name;
        private String accountId;
        private String accountType;
        private String targetBankId;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getAccountId() {
            return accountId;
        }

        public void setAccountId(String accountId) {
            this.accountId = accountId;
        }

        public String getAccountType() {
            return accountType;
        }

        public void setAccountType(String accountType) {
            this.accountType = accountType;
        }

        public String getTargetBankId() {
            return targetBankId;
        }

        public void setTargetBankId(String targetBankId) {
            this.targetBankId = targetBankId;
        }
    }
}
