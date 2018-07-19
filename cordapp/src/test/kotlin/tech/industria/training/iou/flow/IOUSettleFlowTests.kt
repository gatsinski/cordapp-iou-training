package tech.industria.training.iou.flow

import net.corda.core.flows.FlowException
import net.corda.core.node.services.queryBy
import net.corda.finance.POUNDS
import org.junit.Test
import tech.industria.training.iou.state.IOUState
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class IOUSettleFlowTests : IOUFlowTestsBase() {

    @Test fun `settle flow should complete successfully`() {
        var issuanceTransaction = issueIOU(borrower, lender, 10.POUNDS)
        network.waitQuiescent()

        var issuedIOU = issuanceTransaction.tx.outputStates.first() as IOUState
        settleIOU(borrower, issuedIOU.linearId)
        network.waitQuiescent()

        for (node in listOf(borrower, lender)) {
            node.transaction {
                val ious = node.services.vaultService.queryBy<IOUState>().states
                assertEquals(0, ious.size)
            }
        }

    }

    @Test fun `only the borrower can initiate the settle flow`() {
        var issuanceTransaction = issueIOU(borrower, lender, 10.POUNDS)
        network.waitQuiescent()
        var issuedIOU = issuanceTransaction.tx.outputStates.first() as IOUState

        assertFailsWith<FlowException> {
            settleIOU(lender, issuedIOU.linearId)
        }
    }
}