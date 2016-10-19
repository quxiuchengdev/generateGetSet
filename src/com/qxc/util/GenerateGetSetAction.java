package com.qxc.util;

import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.LangDataKeys;
import com.intellij.openapi.actionSystem.PlatformDataKeys;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.CollectionListModel;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @Author quxiucheng
 * @ClassName GenerateGetSetAction
 * @Description
 * @Date 2016/10/19 15:21
 */
public class GenerateGetSetAction extends AnAction {
	private static final String GET = "获取";
	private static final String SET = "设置";

	@Override
	public void actionPerformed (AnActionEvent e) {
		generateGSMethod(getPsiMethodFromContext(e));
	}

	/**
	 * 启动写线程
	 *
	 * @param psiMethod
	 */
	private void generateGSMethod (final PsiClass psiMethod) {
		new WriteCommandAction.Simple(psiMethod.getProject(), psiMethod.getContainingFile()) {
			@Override
			protected void run () throws Throwable {
				createGetSet(psiMethod);
			}
		}.execute();
	}

	private void createGetSet (PsiClass psiClass) {
		//获取所有的字段
		List<PsiField> fields = new CollectionListModel<PsiField>(psiClass.getFields()).getItems();
		if (fields == null) {
			return;
		}
		//获取所有的方法
		List<PsiMethod> list = new CollectionListModel<PsiMethod>(psiClass.getMethods()).getItems();
		Set<String> methodSet = new HashSet<String>();
		for (PsiMethod m : list) {
			methodSet.add(m.getName());
		}
		PsiElementFactory elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());

		for (PsiField field : fields) {
			if (field.getModifierList().hasModifierProperty(PsiModifier.FINAL)) {
				continue;
			}
			String methodText = buildGet(field);
			//创建get方法
			PsiMethod toMethod = elementFactory.createMethodFromText(methodText, psiClass);
			//如果创建过该方法则跳过
			if (methodSet.contains(toMethod.getName())) {
				continue;
			}
			psiClass.add(toMethod);
			//创建set方法
			methodText = buildSet(field);
			elementFactory = JavaPsiFacade.getElementFactory(psiClass.getProject());
			//创建set方法
			toMethod = elementFactory.createMethodFromText(methodText, psiClass);
			if (methodSet.contains(toMethod)) {
				continue;
			}
			psiClass.add(toMethod);
		}
	}

	/**
	 * 设置GET方法
	 * @param field 字段
	 * @return get方法文本
	 */
	private String buildGet (PsiField field) {
		StringBuilder sb = new StringBuilder();
		String doc = format(GET,field);
		if(doc != null){
			sb.append(doc);
		}
		sb.append("public ");
		//判断字段是否是static
		if (field.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
			sb.append("static ");
		}
		sb.append(field.getType().getPresentableText() + " ");
		if (field.getType().getPresentableText().equals("boolean")) {
			sb.append("is");
		} else {
			sb.append("get");
		}
		sb.append(getFirstUpperCase(field.getName()));
		sb.append("(){\n");
		sb.append(" return this." + field.getName() + ";}\n");

		return sb.toString();
	}

	/**
	 * 创建set方法
	 * @param field 字段名称
	 * @return set方法文本
	 */
	private String buildSet (PsiField field) {
		StringBuilder sb = new StringBuilder();
		String doc = format(SET,field);
		if(doc != null){
			sb.append(doc);
		}
		sb.append("public ");
		//判断字段是否是static
		if (field.getModifierList().hasModifierProperty(PsiModifier.STATIC)) {
			sb.append("static ");
		}
		sb.append("void ");
		sb.append("set" + getFirstUpperCase(field.getName()));
		sb.append("(" + field.getType().getPresentableText() + " " + field.getName() + "){\n");
		sb.append("this." + field.getName() + " = " + field.getName() + ";");
		sb.append("}");
		return sb.toString();
	}

	/**
	 * 首字母大写
	 * @param str 字符串
	 * @return
	 */
	private String getFirstUpperCase (String str) {
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}

	private PsiClass getPsiMethodFromContext (AnActionEvent e) {
		PsiElement elementAt = getPsiElement(e);
		if (elementAt == null) {
			return null;

		}
		return PsiTreeUtil.getParentOfType(elementAt, PsiClass.class);
	}

	private PsiElement getPsiElement (AnActionEvent e) {
		PsiFile psiFile = e.getData(LangDataKeys.PSI_FILE);
		Editor editor = e.getData(PlatformDataKeys.EDITOR);
		if (psiFile == null || editor == null) {
			e.getPresentation().setEnabled(false);
			return null;
		}
		//用来获取当前光标处的PsiElement
		int offset = editor.getCaretModel().getOffset();
		return psiFile.findElementAt(offset);
	}

	/**
	 * 格式化get或set方法
	 * @param getOrSetText get或set文本
	 * @param field 字段
	 * @return 格式化后的文本
	 */
	private String format(String getOrSetText,PsiField field){
		//返回信心
		String result = new String();
		//字段注释信息
		String annotation = new String();
		if(field.getDocComment() == null){
			annotation = field.getText().substring(0,field.getText().lastIndexOf("\n")+1);
		}else{
			annotation = field.getDocComment().getText();
		}
		//注释不为空
		if(!"".equals(annotation)){
			//生成注释类型
			String annoType = "1";
			if(annotation.startsWith("//")){
				annoType = "2";
			}
			//取消所有注释
			annotation = annotation.replace("/", "").replace("\\", "").replace("*", "").replace("\n", "").replace(" ", "");
			//生成不同的注释
			if("1".equals(annoType)){
				//get方法
				if(GET.equals(getOrSetText)){
					//字段名称
					String fieldName = field.getName();
					result = "/** " ;
					result = result + "\n* " + getOrSetText + " " +annotation + " ";
					result = result + "\n* @return " + fieldName +" " + annotation +" ";
					result = result +"\n*/";
				}else {
					//set方法
					//字段名称
					String fieldName = field.getName();
					result = "/** " ;
					result = result + "\n* " + getOrSetText + " " +annotation + " ";
					result = result + "\n* @param " + fieldName +" " + annotation +" ";
					result = result +"\n*/";
				}
			}else{
				result = "//" + result + " ";
			}
		}
		return result;
	}

}
