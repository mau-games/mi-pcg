package game.quest.actions;

import game.quest.Action;
import game.quest.ActionType;

/**
 * @author Eric Grevillius
 * @author Elin Olsson
 */
public class GotoAction extends Action {
    public GotoAction() {
    }

    public GotoAction(boolean precondition) {
        super(precondition);
    }

    public GotoAction(ActionType type) {
        super(type);
    }
}
