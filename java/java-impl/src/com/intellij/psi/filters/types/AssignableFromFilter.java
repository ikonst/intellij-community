/*
 * Copyright 2000-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.psi.filters.types;

import com.intellij.psi.*;
import com.intellij.psi.filters.ElementFilter;
import com.intellij.psi.filters.FilterUtil;
import com.intellij.psi.infos.CandidateInfo;
import com.intellij.psi.util.PsiUtil;
import com.intellij.util.ExceptionUtil;
import org.jetbrains.annotations.Nullable;


public class AssignableFromFilter implements ElementFilter {
  private PsiType myType;
  private String myClassName;

  public AssignableFromFilter(PsiType type) {
    myType = type;
  }

  public AssignableFromFilter(final String className) {
    myClassName = className;
  }

  @Override
  public boolean isClassAcceptable(Class hintClass) {
    return true;
  }

  @Override
  public boolean isAcceptable(Object element, PsiElement context) {
    PsiType type = myType;
    if (type == null) {
      JavaPsiFacade psiFacade = JavaPsiFacade.getInstance(context.getProject());
      final PsiClass aClass = psiFacade.findClass(myClassName, context.getResolveScope());
      if (aClass != null) {
        type = psiFacade.getElementFactory().createType(aClass, PsiSubstitutor.EMPTY);
      }
    }
    if (type == null) return false;
    if (element == null) return false;
    if (element instanceof PsiType) return type.isAssignableFrom((PsiType)element);

    PsiSubstitutor substitutor = null;

    if (element instanceof CandidateInfo) {
      final CandidateInfo info = (CandidateInfo)element;
      substitutor = info.getSubstitutor();
      element = info.getElement();
    }

    return isAcceptable((PsiElement)element, context, type, substitutor);
  }

  public static boolean isAcceptable(PsiElement element, PsiElement context, PsiType expectedType, PsiSubstitutor substitutor) {
    if (element instanceof PsiMethod && isReturnTypeInferrable((PsiMethod)element, context, expectedType, substitutor)) {
      return true;
    }

    PsiType typeByElement = FilterUtil.getTypeByElement(element, context);
    if (typeByElement == null) {
      return false;
    }

    if (substitutor != null) {
      typeByElement = substitutor.substitute(typeByElement);
    }

    if (!allowBoxing(context) && (expectedType instanceof PsiPrimitiveType != typeByElement instanceof PsiPrimitiveType)) {
      return false;
    }

    try {
      return expectedType.isAssignableFrom(typeByElement);
    }
    catch (Throwable e) {
      if (ExceptionUtil.getRootCause(e) instanceof PsiInvalidElementAccessException) {
        PsiUtil.ensureValidType(expectedType);
        throw new RuntimeException("Invalid type of " + element.getClass(), e);
      }
      throw e;
    }
  }

  private static boolean allowBoxing(PsiElement place) {
    final PsiElement parent = place.getParent();
    if (parent.getParent() instanceof PsiSynchronizedStatement) {
      final PsiSynchronizedStatement statement = (PsiSynchronizedStatement)parent.getParent();
      if (parent.equals(statement.getLockExpression())) {
        return false;
      }
    }
    return true;
  }

  private static boolean isReturnTypeInferrable(PsiMethod method,
                                                PsiElement place,
                                                PsiType expectedType,
                                                @Nullable PsiSubstitutor substitutor) {
    final PsiResolveHelper helper = JavaPsiFacade.getInstance(method.getProject()).getResolveHelper();
    PsiTypeParameter[] typeParameters = method.getTypeParameters();
    if (typeParameters.length == 0) return false;
    PsiType returnType = method.getReturnType();
    if (substitutor != null) {
      returnType = substitutor.substitute(returnType);
    }
    if (returnType instanceof PsiClassType) {
      PsiClass target = ((PsiClassType)returnType).resolve();
      if (!(target instanceof PsiTypeParameter) && !expectedType.isAssignableFrom(((PsiClassType)returnType).rawType())) {
        return false;
      }
    }
    for (final PsiTypeParameter parameter : typeParameters) {
      final PsiType substitutionForParameter = helper.getSubstitutionForTypeParameter(parameter,
                                                                                      returnType,
                                                                                      expectedType,
                                                                                      false,
                                                                                      PsiUtil.getLanguageLevel(place));
      if (substitutionForParameter != PsiTypes.nullType() &&
          !isImpossibleIntersection(substitutionForParameter) &&
          !extendsImpossibleIntersection(PsiUtil.resolveClassInClassTypeOnly(substitutionForParameter)) &&
          PsiUtil.resolveClassInClassTypeOnly(substitutionForParameter) != parameter) {
        return true;
      }
    }
    return false;
  }

  private static boolean extendsImpossibleIntersection(@Nullable PsiClass psiClass) {
    if (psiClass instanceof PsiTypeParameter) {
      PsiClassType[] supers = psiClass.getExtendsListTypes();
      if (supers.length > 1) {
        return isImpossibleIntersection(PsiIntersectionType.createIntersection(supers));
      }
    }
    return false;
  }

  private static boolean isImpossibleIntersection(PsiType intersection) {
    return intersection instanceof PsiIntersectionType && ((PsiIntersectionType)intersection).getConflictingConjunctsMessage() != null;
  }

  public String toString() {
    return "assignable-from(" + (myType != null ? myType : myClassName) + ")";
  }
}
