// License: GPL. For details, see LICENSE file.
package mergeoverlap.hack;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.RelationMember;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.gui.conflict.tags.RelationMemberConflictDecision;
import org.openstreetmap.josm.gui.conflict.tags.RelationMemberConflictResolverModel;

import mergeoverlap.MergeOverlapAction;

/**
 * This model manages a list of conflicting relation members.
 *
 * It can be used as {@link javax.swing.table.TableModel}.
 */
public class MyRelationMemberConflictResolverModel extends RelationMemberConflictResolverModel {
    /** the property name for the number conflicts managed by this model */
    public static final String NUM_CONFLICTS_PROP = MyRelationMemberConflictResolverModel.class.getName() + ".numConflicts";

    @Override
    protected String getProperty() {
        return NUM_CONFLICTS_PROP;
    }

    @Override
    protected void populate(Relation relation, OsmPrimitive primitive) {
        throw new UnsupportedOperationException("Use populate(Relation, OsmPrimitive, Map<Way, Way>) instead");
    }

    /**
     * Populates the model with the members of the relation <code>relation</code>
     * referring to <code>way</code>.
     *
     * @param relation the parent relation
     * @param way the child way
     */
    protected void populate(Relation relation, Way way, Map<Way, Way> oldWays) {
        for (int i =0; i<relation.getMembersCount();i++) {
        	RelationMember mb = relation.getMember(i);
            if (mb.isWay() && MergeOverlapAction.getOld(mb.getWay(), oldWays) == MergeOverlapAction.getOld(way, oldWays)) {
                decisions.add(new RelationMemberConflictDecision(relation, i));
            }
        }
    }

    @Override
    public void populate(Collection<Relation> relations, Collection<? extends OsmPrimitive> memberPrimitives) {
        throw new UnsupportedOperationException(
                "Use populate(Collection<Relation>, Collection<? extends OsmPrimitive>, Map<Way, Way>) instead");
    }

    /**
     * Populates the model with the relation members belonging to one of the relations in <code>relations</code>
     * and referring to one of the primitives in <code>memberPrimitives</code>.
     *
     * @param relations  the parent relations. Empty list assumed if null.
     * @param memberWays the child ways. Empty list assumed if null.
     */
    public void populate(Collection<Relation> relations, Collection<Way> memberWays, Map<Way, Way> oldWays) {
        decisions.clear();
        relations = relations == null ? new LinkedList<>() : relations;
        memberWays = memberWays == null ? new LinkedList<>() : memberWays;
        for (Relation r : relations) {
            for (Way w : memberWays) {
                populate(r, w, oldWays);
            }
        }
        this.relations = relations;
        this.primitives = memberWays;
        refresh();
    }

    @Override
    protected Command buildResolveCommand(Relation relation, OsmPrimitive newPrimitive) {
        throw new UnsupportedOperationException(
                "Use buildResolveCorrespondance(Relation, OsmPrimitive, Map<Relation, Relation>, Map<Way, Way>) instead");
    }

    protected void buildResolveCorrespondance(
            Relation relation, OsmPrimitive newPrimitive, Map<Relation, Relation> newRelations, Map<Way, Way> oldWays) {

        List<RelationMember> relationsMembers = relation.getMembers();
        Relation modifiedRelation = MergeOverlapAction.getNew(relation, newRelations);
        modifiedRelation.setMembers(null);
        for (int i=0; i < relationsMembers.size(); i++) {
            RelationMember rm = relationsMembers.get(i);
            RelationMemberConflictDecision decision = getDecision(relation, i);
            if (decision == null) {
                modifiedRelation.addMember(rm);
            } else {
                switch(decision.getDecision()) {
                case KEEP:
                    if (newPrimitive instanceof Way) {
                        modifiedRelation.addMember(new RelationMember(decision.getRole(),
                                MergeOverlapAction.getOld((Way)newPrimitive, oldWays)));
                    } else {
                        modifiedRelation.addMember(new RelationMember(decision.getRole(), newPrimitive));
                    }
                    break;
                case REMOVE:
                    // do nothing
                    break;
                case UNDECIDED:
                    // FIXME: this is an error
                    break;
                }
            }
        }
    }

    @Override
    public List<Command> buildResolutionCommands(OsmPrimitive newPrimitive) {
        throw new UnsupportedOperationException(
                "Use buildRelationCorrespondance(OsmPrimitive, Map<Relation, Relation>, Map<Way, Way>) instead");
    }

    /**
     * Builds a collection of commands executing the decisions made in this model.
     *
     * @param newPrimitive the primitive which members shall refer to if the
     * decision is {@see RelationMemberConflictDecisionType#REPLACE}
     */
    public void buildRelationCorrespondance(OsmPrimitive newPrimitive, Map<Relation, Relation> newRelations, Map<Way, Way> oldWays) {
        for (Relation relation : relations) {
            buildResolveCorrespondance(relation, newPrimitive, newRelations, oldWays);
        }
    }
}
