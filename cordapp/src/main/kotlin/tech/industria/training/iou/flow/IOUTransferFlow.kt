package tech.industria.training.iou.flow

import co.paralleluniverse.fibers.Suspendable

import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

import tech.industria.training.iou.contract.IOUContract
import tech.industria.training.iou.state.IOUState

class IOUTransferFlow {
    @InitiatingFlow
    @StartableByRPC
    class Initiator(
            private val linearId: UniqueIdentifier,
            private val newLender: Party
    ) : FlowLogic<SignedTransaction>() {
        @Suspendable
        override fun call(): SignedTransaction {
            val notary = serviceHub.networkMapCache.notaryIdentities.first()
            val stateAndRef = getIOUByLinearId(linearId)
            val issuedIOU = stateAndRef.state.data

            val lenderIdentity = serviceHub.identityService.requireWellKnownPartyFromAnonymous(issuedIOU.lender)

            if (ourIdentity != lenderIdentity) {
                throw FlowException("Transfer IOU flow must be initiated by the lender.")
            }

            val transferredIOU = issuedIOU.copy(lender = newLender)
            val signers = issuedIOU.participants + newLender
            val signerKeys = signers.map { it.owningKey }

            val command = Command(IOUContract.Commands.Transfer(), signerKeys)

            val builder = TransactionBuilder(notary = notary)
                    .addInputState(stateAndRef)
                    .addOutputState(transferredIOU, IOUContract.PROGRAM_ID)
                    .addCommand(command)

            builder.verify(serviceHub)
            val ptx = serviceHub.signInitialTransaction(builder)
            val sessions = (transferredIOU.participants).map { initiateFlow(it) }.toSet()
            val stx = subFlow(CollectSignaturesFlow(ptx, sessions, CollectSignaturesFlow.tracker()))

            return subFlow(FinalityFlow(stx))
        }

        @Suspendable
        fun getIOUByLinearId(linearId: UniqueIdentifier): StateAndRef<IOUState> {
            val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                    linearId = listOf(linearId),
                    status = Vault.StateStatus.UNCONSUMED
            )
            return serviceHub.vaultService.queryBy<IOUState>(queryCriteria).states.singleOrNull()
                    ?: throw FlowException("Obligation with id $linearId not found.")
        }
    }

    @InitiatedBy(IOUTransferFlow.Initiator::class)
    class Responder(val otherPartySession: FlowSession) : FlowLogic<Unit>() {
        @Suspendable
        override fun call() {
            val signTransactionFlow = object : SignTransactionFlow(otherPartySession, SignTransactionFlow.tracker()) {
                override fun checkTransaction(stx: SignedTransaction) = requireThat {
                    val output = stx.tx.outputs.single().data
                    "This must be an IOU transaction" using (output is IOUState)
                }
            }

            subFlow(signTransactionFlow)
        }
    }
}