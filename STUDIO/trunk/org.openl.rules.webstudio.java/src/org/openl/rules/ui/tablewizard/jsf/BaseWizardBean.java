package org.openl.rules.ui.tablewizard.jsf;

/**
 * @author Aliaksandr Antonik.
 */
public abstract class BaseWizardBean {
    private int step;
    private int maxVisitedStep;

    public void cancel() {
        onCancel();
    }

    public int getMaxVisitedStep() {
        return maxVisitedStep;
    }

    public String getName() {
        return "page";
    }

    public int getStep() {
        return step;
    }

    public String next() {
        if (getStep() == getMaxVisitedStep()) {
            onStepFirstVisit(getStep() + 1);
        }
        return "next";
    }

    protected abstract void onCancel();

    protected abstract void onStart();

    protected abstract void onFinish() throws Exception;

    protected void onStepFirstVisit(int step) {
    }

    public String prev() {
        return "prev";
    }

    public void setStep(int step) {
        this.step = step;
        maxVisitedStep = Math.max(step, maxVisitedStep);
    }

    public String start() {
        maxVisitedStep = step = 0;
        onStart();
        return getName();
    }

    public String finish() throws Exception {
        onFinish();
        return null;
    }

}
