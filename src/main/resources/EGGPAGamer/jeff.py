import random

# from org.ggp.base.util.statemachine import MachineState
from com.xhaus.jyson import JysonCodec as json
from org.ggp.base.util.statemachine.implementation.prover import ProverStateMachine
from org.ggp.base.util.statemachine.cache import CachedStateMachine
from org.ggp.base.player.gamer.statemachine import StateMachineGamer


class Jeff(StateMachineGamer):

    def getName(self):
        pass

    def stateMachineMetaGame(self, timeout):
        print('dudududududud')
        self.timeout = timeout
        pass

    def stateMachineSelectMove(self, timeout):
        moves = self.getStateMachine().getLegalMoves(self.getCurrentState(),
                                                     self.getRole())
        selection = random.choice(moves)
        return selection

    def stateMachineStop(self):
        pass


    def stateMachineAbort(self):
        pass

    def getInitialStateMachine(self):
        return CachedStateMachine(ProverStateMachine())

    def getLegalMoves(self, role):
        return self.getStateMachine().getLegalMoves(self.getCurrentState(),
                                                    self.getRole())

    def getEvaluation(self):
        du = json.dumps([(x, i) for x, i in enumerate(self.getStateMachine()
                                                      .getLegalMoves(self.getCurrentState(),
                                                                     self.getRole()))])
        print(du)
        return du
