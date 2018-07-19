package tech.industria.training.iou.flow

import org.junit.Before
import org.junit.Test

import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

import net.corda.core.flows.FlowException
import net.corda.finance.POUNDS
import net.corda.testing.node.StartedMockNode

import tech.industria.training.iou.state.IOUState

class IOUTransferFlowTests: IOUFlowTestsBase() {
    private lateinit var newLender: StartedMockNode

    @Before
    fun transferFlowSetup() {
        newLender = network.createNode()
    }

    @Test
    fun `IOU transfer should complete successfully`() {
        val issuanceTransaction = issueIOU(borrower, lender, 10.POUNDS)
        network.waitQuiescent()
        val issuedIOU = issuanceTransaction.tx.outputStates.single() as IOUState

        val transferTransaction = transferIOU(issuedIOU.linearId, lender, newLender)
        network.waitQuiescent()

        val transferredIOU = transferTransaction.tx.outputStates.single() as IOUState

        assertEquals(issuedIOU.copy(lender = transferredIOU.lender), transferredIOU)

        val lenderIOUState = lender.services.loadState(transferTransaction.tx.outRef<IOUState>(0).ref).data
        val newLenderIOUState = newLender.services.loadState(transferTransaction.tx.outRef<IOUState>(0).ref).data
        val borrowerIOUState = borrower.services.loadState(transferTransaction.tx.outRef<IOUState>(0).ref).data

        assertEquals(lenderIOUState, newLenderIOUState)
        assertEquals(lenderIOUState, borrowerIOUState)
    }

    @Test
    fun `IOU transfer can only be initiated by the lender`() {
        val issuanceTransaction = issueIOU(borrower, lender, 10.POUNDS)
        network.waitQuiescent()

        val issuedIOU = issuanceTransaction.tx.outputStates.single() as IOUState

        assertFailsWith<FlowException> {
            transferIOU(issuedIOU.linearId, borrower, newLender)
        }
    }
}