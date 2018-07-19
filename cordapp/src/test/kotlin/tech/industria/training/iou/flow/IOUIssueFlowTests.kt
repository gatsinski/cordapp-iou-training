package tech.industria.training.iou.flow

import net.corda.core.contracts.TransactionVerificationException
import net.corda.core.contracts.requireThat
import net.corda.core.flows.FlowException
import net.corda.core.node.services.queryBy
import net.corda.finance.POUNDS
import net.corda.testing.core.singleIdentity
import org.junit.Test
import tech.industria.training.iou.state.IOUState
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IOUIssueFlowTests : IOUFlowTestsBase() {

    @Test
    fun `successfully issue IOU`() {
        val signedTx = issueIOU(borrower, lender, 10.POUNDS)
        network.waitQuiescent()

        val aIOU = borrower.services.loadState(signedTx.tx.outRef<IOUState>(0).ref).data as IOUState
        val bIOU = lender.services.loadState(signedTx.tx.outRef<IOUState>(0).ref).data as IOUState

        assertEquals(aIOU, bIOU)
    }

    @Test fun `IOU should be recorded by both parties`() {
        val iouValue = 10.POUNDS
        issueIOU(lender, borrower, iouValue)
        network.waitQuiescent()

        for (node in listOf(borrower, lender)) {
            node.transaction {
                val ious = node.services.vaultService.queryBy<IOUState>().states
                assertEquals(1, ious.size)
                val recordedState = ious.single().state.data
                assertEquals(recordedState.amount, iouValue)
                assertEquals(recordedState.lender, borrower.info.singleIdentity())
                assertEquals(recordedState.borrower, lender.info.singleIdentity())
            }
        }
    }

    @Test
    fun `flow returns transaction signed by both parties`() {
        val signedTx = issueIOU(borrower, lender, 10.POUNDS)
        network.waitQuiescent()
        requireThat {
            "Both parties should sign" using (
                    signedTx.sigs.map { it.by }.toSet() ==
                            listOf(borrower, lender).map { it.info.singleIdentity().owningKey }.toSet())
        }
    }

    @Test
    fun `amount should be positive`() {
        assertFailsWith<TransactionVerificationException> {
            issueIOU(borrower, lender, 0.POUNDS)
        }
    }

    @Test
    fun `amount should not be too high`() {
        assertFailsWith<FlowException> {
            issueIOU(borrower, lender, 101.POUNDS)
        }
    }
}