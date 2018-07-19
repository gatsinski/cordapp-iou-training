package tech.industria.training.iou.flow

import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.StartedMockNode
import org.junit.After
import org.junit.Before
import java.util.*

abstract class IOUFlowTestsBase {
    protected lateinit var network: MockNetwork
    protected lateinit var borrower: StartedMockNode
    protected lateinit var lender: StartedMockNode

    @Before
    fun setup() {
        network = MockNetwork(listOf("tech.industria.training.iou"), threadPerNode = true)
        borrower = network.createNode()
        lender = network.createNode()
        listOf(borrower, lender).forEach {
            it.registerInitiatedFlow(IOUIssueFlow.Responder::class.java)
        }
    }

    @After
    fun tearDown() {
        network.stopNodes()
    }

    protected fun issueIOU(borrower: StartedMockNode,
                           lender: StartedMockNode,
                           amount: Amount<Currency>): SignedTransaction {
        val lenderIdentity = lender.info.legalIdentities.first()
        val flow = IOUIssueFlow.Initiator(amount, lenderIdentity)
        return borrower.startFlow(flow).getOrThrow()
    }

    protected fun settleIOU(borrower: StartedMockNode,
                            linearId: UniqueIdentifier): SignedTransaction {
        val flow = IOUSettleFlow.Initiator(linearId)
        return borrower.startFlow(flow).getOrThrow()
    }

    protected fun transferIOU(linearId: UniqueIdentifier,
                              oldLender: StartedMockNode,
                              newLender: StartedMockNode): SignedTransaction {
        val newLenderIdentity = newLender.info.legalIdentities.first()
        val flow = IOUTransferFlow.Initiator(linearId, newLenderIdentity)
        return oldLender.startFlow(flow).getOrThrow()
    }

}