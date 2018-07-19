package tech.industria.training.iou.flow

import net.corda.core.flows.FlowException
import net.corda.finance.POUNDS
import org.junit.Test
import tech.industria.training.iou.state.IOUState
import kotlin.test.assertFailsWith

class IOUSettleFlowTests : IOUFlowTestsBase() {

    @Test fun `only the borrower can initiate the settle flow`() {
        var issuanceTransaction = issueIOU(borrower, lender, 10.POUNDS)
        network.waitQuiescent()
        var issuedIOU = issuanceTransaction.tx.outputStates.first() as IOUState

        assertFailsWith<FlowException> {
            settleIOU(lender, issuedIOU.linearId)
        }
    }
}